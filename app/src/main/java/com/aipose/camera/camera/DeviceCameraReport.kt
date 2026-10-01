package com.aipose.camera.camera

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build

/** Read-only capability inventory. Does not enable unsupported controls or claim OEM camera parity. */
object DeviceCameraReport {
    fun read(context:Context):String = try {
        val manager=context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val rows=manager.cameraIdList.map {id->
            val c=manager.getCameraCharacteristics(id)
            val facing=when(c.get(CameraCharacteristics.LENS_FACING)){CameraCharacteristics.LENS_FACING_BACK->"后置";CameraCharacteristics.LENS_FACING_FRONT->"前置";else->"其他"}
            val manual=c.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR)==true
            val iso=c.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
            "$facing $id：${if(manual) "系统报告手动传感器能力" else "未报告手动传感器能力"}${if(iso!=null) "，ISO ${iso.lower}–${iso.upper}" else ""}"
        }
        "${Build.MANUFACTURER} ${Build.MODEL}\n"+rows.joinToString("\n")+"\n系统报告不代表本App已启用。本版本只手调曝光补偿/变焦/对焦；ISO、快门、相机白平衡自动。未启用厂家HDR/夜景扩展，不保证与原生相机相同画质。"
    } catch(e:Exception) {"暂不能读取设备能力；请先允许相机权限。ISO、快门和相机白平衡仍由系统自动处理。"}
}
