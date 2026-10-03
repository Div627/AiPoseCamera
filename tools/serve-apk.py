#!/usr/bin/env python3
"""Serve only explicitly listed release artifacts, including APK byte ranges."""
import argparse
import json
import mimetypes
import re
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlsplit

ROOT = Path(__file__).resolve().parent.parent / "distribution"

class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        self.respond(send_body=True)

    def do_HEAD(self):
        self.respond(send_body=False)

    def respond(self, send_body):
        manifest_path = ROOT / "version.json"
        try:
            manifest = json.loads(manifest_path.read_text())
            version = manifest["versionName"]
            if not re.fullmatch(r"\d+\.\d+\.\d+(?:-beta\.\d+)?", version):
                raise ValueError("Invalid version")
        except (OSError, ValueError, KeyError):
            self.send_error(503, "Release unavailable")
            return
        apk_name = f"yingke-camera-{version}.apk"
        qr_name = f"install-qr-{version}.png"
        backup_qr_name = f"install-backup-qr-{version}.png"
        lan_qr_name = f"install-lan-qr-{version}.png"
        allowed = {"/": "index.html", "/index.html": "index.html", "/version.json": "version.json",
                   "/SHA256SUMS.txt": "SHA256SUMS.txt", "/" + apk_name: apk_name, "/" + qr_name: qr_name, "/" + lan_qr_name: lan_qr_name, "/" + backup_qr_name: backup_qr_name}
        filename = allowed.get(urlsplit(self.path).path)
        if filename is None or not (ROOT / filename).is_file():
            self.send_error(404)
            return
        path = ROOT / filename
        size = path.stat().st_size
        start, end, status = 0, size - 1, 200
        requested = self.headers.get("Range") if filename == apk_name else None
        if requested:
            match = re.fullmatch(r"bytes=(\d*)-(\d*)", requested)
            if match and (match[1] or match[2]):
                if match[1]:
                    start = int(match[1])
                    end = min(int(match[2]) if match[2] else size - 1, size - 1)
                else:
                    start = max(size - int(match[2]), 0)
                status = 206
            if not match or not 0 <= start <= end < size or not (match[1] or match[2]):
                self.send_response(416)
                self.send_header("Content-Range", f"bytes */{size}")
                self.send_header("Content-Length", "0")
                self.end_headers()
                return
        self.send_response(status)
        self.send_header("Content-Type", "application/vnd.android.package-archive" if filename == apk_name else mimetypes.guess_type(filename)[0] or "application/octet-stream")
        self.send_header("Content-Length", str(end - start + 1))
        self.send_header("Cache-Control", "no-store")
        self.send_header("X-Content-Type-Options", "nosniff")
        if filename == apk_name:
            self.send_header("Accept-Ranges", "bytes")
        if status == 206:
            self.send_header("Content-Range", f"bytes {start}-{end}/{size}")
        self.end_headers()
        if send_body:
            try:
                with path.open("rb") as stream:
                    stream.seek(start)
                    remaining = end - start + 1
                    while remaining:
                        block = stream.read(min(65536, remaining))
                        if not block:
                            break
                        self.wfile.write(block)
                        remaining -= len(block)
            except (BrokenPipeError, ConnectionResetError):
                pass

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=8765)
    args = parser.parse_args()
    ThreadingHTTPServer((args.host, args.port), Handler).serve_forever()
