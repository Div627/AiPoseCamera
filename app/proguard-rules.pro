# MediaPipe JNI calls these Java interfaces, methods and fields by name.
# Source: mediapipe/java/com/google/mediapipe/framework/proguard.pgcfg
# tasks-vision 0.10.32 no longer ships the previous consumer rules.
-keep public interface com.google.mediapipe.framework.* {
    public *;
}
-keep public class com.google.mediapipe.framework.Packet {
    public static *** create(***);
    public long getNativeHandle();
    public void release();
}
-keep public class com.google.mediapipe.framework.PacketCreator {
    *** releaseWithSyncToken(...);
}
-keep public class com.google.mediapipe.framework.MediaPipeException {
    <init>(int, byte[]);
}
-keep class com.google.mediapipe.framework.ProtoUtil$SerializedMessage { *; }
