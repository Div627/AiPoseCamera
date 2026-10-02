package com.aipose.camera.update

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.ProtocolException
import java.net.UnknownHostException

enum class UpdateProblem(val message: String) {
    NETWORK("无法连接下载服务器，请切换 Wi-Fi 或移动网络后重试"),
    TIMEOUT("下载连接超时，请切换网络后重试"),
    NOT_FOUND("更新链接已失效，请扫描最新版本的二维码"),
    SERVER("下载服务暂不可用，请稍后重试"),
    NOT_APK("链接未返回有效安装包，请扫描最新更新二维码"),
    INCOMPLETE("安装包下载不完整，请重试"),
    STORAGE("无法保存安装包，请检查手机剩余空间"),
    INVALID_APK("安装包无法校验，请重新下载"),
    WRONG_APP("这不是映刻相机的安装包，请扫描正确二维码"),
    CURRENT_VERSION("手机已安装此版本或更高版本，无需更新"),
    SIGNATURE("安装包与当前版本签名不一致，无法覆盖安装，请联系开发者"),
}

class UpdateFailure(val problem: UpdateProblem, cause: Throwable? = null) : IOException(problem.name, cause)

object UpdateErrors {
    fun classify(error: Throwable): UpdateProblem = when (error) {
        is UpdateFailure -> error.problem
        is ProtocolException -> UpdateProblem.INCOMPLETE
        is SocketTimeoutException -> UpdateProblem.TIMEOUT
        is UnknownHostException, is IOException -> UpdateProblem.NETWORK
        else -> UpdateProblem.INVALID_APK
    }
    fun http(status: Int) = if (status == 404 || status == 410) UpdateProblem.NOT_FOUND else UpdateProblem.SERVER
}

object UpdatePackagePolicy {
    fun check(name: String, expectedName: String, version: Long, installedVersion: Long,
              installedSigners: Set<String>, archiveSigners: Set<String>, allowLineage: Boolean = false): UpdateProblem? = when {
        name != expectedName -> UpdateProblem.WRONG_APP
        version <= installedVersion -> UpdateProblem.CURRENT_VERSION
        installedSigners.isEmpty() || (if (allowLineage) !archiveSigners.containsAll(installedSigners) else archiveSigners != installedSigners) -> UpdateProblem.SIGNATURE
        else -> null
    }
}
