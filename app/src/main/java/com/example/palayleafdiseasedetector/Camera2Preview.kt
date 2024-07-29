package com.example.palayleafdiseasedetector

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.AttributeSet
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.util.Collections
import java.util.concurrent.Semaphore

@Suppress("DEPRECATION")
class Camera2Preview @JvmOverloads constructor(
    private val mContext: Context,
    attrs: AttributeSet? = null
) : TextureView(mContext, attrs) {

    private lateinit var cameraDevice: CameraDevice
    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private lateinit var captureSession: CameraCaptureSession
    private lateinit var previewSize: Size
    private var isCameraOpened = false

    private lateinit var backgroundThread: HandlerThread
    private lateinit var backgroundHandler: Handler
    private val cameraOpenCloseLock = Semaphore(1)

    init {
        setupBackgroundThread()
        setupSurfaceTextureListener()
    }

    private fun setupBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").apply { start() }
        backgroundHandler = Handler(backgroundThread.looper)
    }

    private fun setupSurfaceTextureListener() {
        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                openCamera()
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = false

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }
    }

    fun openCamera() {
        if (ContextCompat.checkSelfPermission(mContext, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val manager = mContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            try {
                val cameraIdList = manager.cameraIdList
                for (cameraId in cameraIdList) {
                    val characteristics = manager.getCameraCharacteristics(cameraId)
                    if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
                        previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture::class.java), width, height)
                        manager.openCamera(cameraId, stateCallback, null)
                        break
                    }
                }
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        } else {
            // Handle the case when CAMERA permission is not granted
            Toast.makeText(mContext, "Camera permission not granted", Toast.LENGTH_SHORT).show()
        }
    }

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            createCameraPreviewSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            camera.close()
            isCameraOpened = false
        }

        override fun onError(camera: CameraDevice, error: Int) {
            camera.close()
            Toast.makeText(mContext, "Camera error: $error", Toast.LENGTH_SHORT).show()
            isCameraOpened = false
        }
    }

    private fun createCameraPreviewSession() {
        try {
            val texture = surfaceTexture ?: throw IllegalStateException("SurfaceTexture is null")
            val width = previewSize.width
            val height = previewSize.height

            // Create ImageReader instance for capturing high-resolution JPEG images
            val imageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 2)
            imageReader.setOnImageAvailableListener(imageAvailableListener, backgroundHandler)
            val imageSurface = imageReader.surface

            // Set up the texture buffer size
            texture.setDefaultBufferSize(width, height)

            // Create a Surface from the TextureView's SurfaceTexture
            val surface = Surface(texture)

            // Create a capture request builder for the camera device
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

            // Add the surface targets to the capture request builder
            captureRequestBuilder.addTarget(surface)
            captureRequestBuilder.addTarget(imageSurface)

            // Create a capture session for camera preview
            cameraDevice.createCaptureSession(
                listOf(surface, imageSurface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        captureRequestBuilder.set(
                            CaptureRequest.CONTROL_MODE,
                            CameraMetadata.CONTROL_MODE_AUTO
                        )
                        try {
                            // Start displaying the camera preview
                            captureSession.setRepeatingRequest(
                                captureRequestBuilder.build(),
                                null,
                                backgroundHandler
                            )
                            isCameraOpened = true
                        } catch (e: CameraAccessException) {
                            e.printStackTrace()
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Toast.makeText(
                            mContext,
                            "Failed to configure camera",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                null
            )
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun chooseOptimalSize(choices: Array<Size>, width: Int, height: Int): Size {
        val bigEnough = ArrayList<Size>()
        for (option in choices) {
            if (option.width >= width && option.height >= height) {
                bigEnough.add(option)
            }
        }
        return if (bigEnough.isNotEmpty()) {
            Collections.min(bigEnough) { lhs, rhs -> lhs.width * lhs.height - rhs.width * rhs.height }
        } else {
            choices[0]
        }
    }

    fun startCamera() {
        openCamera()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cameraOpenCloseLock.release()
        cameraDevice.close()
        backgroundThread.quitSafely()
    }

    private val imageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        val image = reader.acquireLatestImage()
        // Process the captured image here
        image?.close()
    }
}
