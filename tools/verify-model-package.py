#!/usr/bin/env python3
"""Validate packaged model bytes and MediaPipe's ARM64 ELF page alignment."""
import hashlib
import struct
import sys
from pathlib import Path
from zipfile import ZIP_STORED, ZipFile

apk = Path(sys.argv[1])
root = Path(__file__).resolve().parent.parent
with ZipFile(apk) as package:
    for name in ("pose_landmarker_lite.task", "face_detector_short_range.tflite", "magic_touch.tflite"):
        asset = "assets/" + name
        assert package.getinfo(asset).compress_type == ZIP_STORED, f"Compressed model: {name}"
        expected = (root / "app/src/main/assets" / name).read_bytes()
        assert hashlib.sha256(package.read(asset)).digest() == hashlib.sha256(expected).digest(), f"Model changed: {name}"
    library = package.read("lib/arm64-v8a/libmediapipe_tasks_vision_jni.so")
    assert library[:6] == b"\x7fELF\x02\x01", "Expected little-endian ELF64"
    offset = struct.unpack_from("<Q", library, 32)[0]
    entry_size, count = struct.unpack_from("<HH", library, 54)
    loads = 0
    for index in range(count):
        kind, _, file_offset, address, _, _, _, alignment = struct.unpack_from("<IIQQQQQQ", library, offset + index * entry_size)
        if kind == 1:
            loads += 1
            assert alignment >= 16384, "MediaPipe LOAD alignment below 16 KB"
            assert file_offset % 16384 == address % 16384, "MediaPipe LOAD address mismatch"
    assert loads, "No ELF LOAD segments"
print("Verified: unchanged uncompressed models; MediaPipe ARM64 ELF 16 KB alignment")
