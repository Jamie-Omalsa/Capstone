
import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ImageClassifier(private val context: Context) {
    private val interpreter: Interpreter

    init {
        // Load the TFLite model
        val model = loadModelFile(context, "model.tflite")
        interpreter = Interpreter(model)
    }

    private fun loadModelFile(context: Context, modelFileName: String): ByteBuffer {
        val assetManager = context.assets
        val inputStream: InputStream = assetManager.open(modelFileName)
        val byteBuffer = ByteBuffer.allocateDirect(inputStream.available()).apply {
            order(ByteOrder.nativeOrder())
            inputStream.use { stream ->
                put(stream.readBytes())
            }
        }
        return byteBuffer
    }

    fun classify(image: Bitmap): Pair<String, Float> {
        // Preprocess the image
        val tensorImage = preprocessImage(image)

        // Create input tensor buffer
        val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
        inputFeature0.loadBuffer(tensorImage.buffer) // Load the processed image data

        // Create output tensor buffer
        val outputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 4), DataType.FLOAT32) // Assuming 4 classes

        // Run the model
        interpreter.run(inputFeature0.buffer, outputFeature0.buffer.rewind())

        // Get the results
        val probabilities = outputFeature0.floatArray
        val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: -1
        val confidence = probabilities[maxIndex]

        // Map index to label
        val labels = arrayOf("Mango leaf", "Human", "Cat", "Dog")
        val label = labels[maxIndex]

        return Pair(label, confidence)
    }

    private fun preprocessImage(image: Bitmap): TensorImage {
        // Resize the image to the size expected by the model
        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(image)

        // Normalize the image (scaling pixel values to the range [0, 1])
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(0f, 1f))
            .build()

        return imageProcessor.process(tensorImage)
    }

    fun close() {
        interpreter.close()
    }
}
