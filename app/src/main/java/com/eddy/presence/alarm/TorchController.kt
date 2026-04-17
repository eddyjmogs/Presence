package com.eddy.presence.alarm

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager

object TorchController {

    fun turnOn(context: Context) = setTorch(context, true)
    fun turnOff(context: Context) = setTorch(context, false)

    private fun setTorch(context: Context, on: Boolean) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = flashCameraId(cameraManager) ?: return
        runCatching { cameraManager.setTorchMode(cameraId, on) }
        // runCatching swallows CameraAccessException — if another app holds the camera,
        // torch silently fails rather than crashing, which is acceptable for this feature.
    }

    private fun flashCameraId(cameraManager: CameraManager): String? =
        cameraManager.cameraIdList.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
}
