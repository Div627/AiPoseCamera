package com.aipose.camera.diagnostics
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
class DiagnosticStoreTest {
    @Test fun excludesExceptionMessagesAndNonFiniteNumbers() {
        val dir=Files.createTempDirectory("diagnostics").toFile()
        try {
            val store=DiagnosticStore(dir)
            store.append(DiagnosticStore.Event.MODEL_ERROR,mapOf(DiagnosticStore.Field.COUNT to 2,DiagnosticStore.Field.STATUS to Double.NaN),IllegalStateException("secret-key https://private.example"))
            val text=store.snapshot().values.joinToString()
            assertTrue(text.contains("IllegalStateException"));assertTrue(text.contains("COUNT"));assertFalse(text.contains("secret-key"));assertFalse(text.contains("private.example"));assertFalse(text.contains("NaN"))
        } finally {dir.deleteRecursively()}
    }
    @Test fun rotationKeepsRecentHistoryBounded() {
        val dir=Files.createTempDirectory("diagnostics").toFile()
        try {val store=DiagnosticStore(dir,200);repeat(100){store.append(DiagnosticStore.Event.FRAME)};assertEquals(2,dir.listFiles()!!.size);assertTrue(dir.listFiles()!!.sumOf{it.length()}<600);assertTrue(store.snapshot().values.all{it.isNotBlank()})} finally {dir.deleteRecursively()}
    }
}
