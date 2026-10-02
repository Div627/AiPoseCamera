package com.aipose.camera.camera

import org.junit.Assert.*
import org.junit.Test

class CaptureQueueTest {
    @Test fun shutterReleasesAfterOriginalWhileEditRemainsQueued() {
        val queue=CaptureQueue();val first=queue.reserve()!!
        assertTrue(queue.capturing);assertNull(queue.reserve())
        assertTrue(queue.publish(first));assertFalse(queue.capturing);assertEquals(1,queue.size)
        assertNotNull(queue.reserve())
    }
    @Test fun pendingCopiesHaveBoundedCapacity() {
        val queue=CaptureQueue();val a=queue.reserve()!!;queue.publish(a)
        val b=queue.reserve()!!;queue.publish(b)
        assertEquals(2,queue.size);assertNull(queue.reserve())
        queue.complete(a);assertNotNull(queue.reserve())
    }
    @Test fun olderCompletionCannotUnlockNewerCapture() {
        val queue=CaptureQueue();val a=queue.reserve()!!;queue.publish(a)
        val b=queue.reserve()!!;queue.complete(a)
        assertTrue(queue.capturing);assertNull(queue.reserve())
        queue.publish(b);assertFalse(queue.capturing);assertNotNull(queue.reserve())
    }
    @Test fun oldPublishedOrEditedPhotoCannotReplaceLatestShot() {
        val queue=CaptureQueue();val a=queue.reserve()!!;queue.publish(a)
        val b=queue.reserve()!!;queue.publish(b)
        assertFalse(queue.publish(a));queue.complete(a)
        assertTrue(queue.isLatest(b));assertFalse(queue.isLatest(a))
    }
    @Test fun captureFailureReleasesSlotAndShutterWithoutReplacingPhoto() {
        val queue=CaptureQueue();val a=queue.reserve()!!;queue.publish(a);queue.complete(a)
        val failed=queue.reserve()!!;queue.complete(failed)
        assertTrue(queue.available);assertTrue(queue.isLatest(a));assertFalse(queue.complete(failed))
    }
    @Test fun unknownPublicationCannotCreateAnInvalidPreview() {
        val queue=CaptureQueue();assertFalse(queue.publish(100));assertEquals(0L,queue.latest)
    }
}
