package com.aipose.camera.update
import org.junit.Assert.*
import org.junit.Test

class ScannerFlowTest {
    private val apk = "https://example.com/camera.apk"
    @Test fun directApk() { assertEquals(apk, UpdateLink.parse(apk)) }
    @Test fun signedLinkAndFragment() { assertEquals("$apk?token=abc", UpdateLink.parse("$apk?token=abc#download")) }
    @Test fun trimsClipboardWhitespace() { assertEquals(apk,UpdateLink.parse("\n \uFEFF$apk \r\n")) }
    @Test fun uppercaseExtension() { assertNotNull(UpdateLink.parse("https://example.com/Camera.APK")) }
    @Test fun encodedExtension() { assertNotNull(UpdateLink.parse("https://example.com/camera%2Eapk")) }
    @Test fun invalidQrDoesNotLockScanner() {
        val s=ScanSession(); assertNull(s.deliver("hello",true));assertEquals(apk,s.deliver(apk,true))
    }
    @Test fun rejectsNonApkAndUnsafeSchemes() {
        for(v in listOf("http://example.com/a.apk","file:///a.apk","javascript:a.apk","https://example.com/?name=a.apk","https://user:pw@example.com/a.apk","https://example.com/a.apk/")) assertNull(v,UpdateLink.parse(v))
    }
    @Test fun oneFrameInFlight() { val s=ScanSession();assertTrue(s.begin());assertFalse(s.begin());s.finish();assertTrue(s.begin()) }
    @Test fun repeatedDetectionDeliversOnce() { val s=ScanSession();assertEquals(apk,s.deliver(apk,true));assertNull(s.deliver(apk,true));assertFalse(s.begin()) }
    @Test fun leavingScreenSuppressesLateResult() { val s=ScanSession();assertTrue(s.begin());s.close();assertNull(s.deliver(apk,true));s.finish();assertFalse(s.begin()) }
    @Test fun backgroundResultDoesNotConsumeValidCode() { val s=ScanSession();assertNull(s.deliver(apk,false));assertEquals(apk,s.deliver(apk,true)) }
    @Test fun reenterUsesFreshSession() { val s=ScanSession();s.deliver(apk,true);s.close();assertEquals(apk,ScanSession().deliver(apk,true)) }
}
