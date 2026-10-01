#!/usr/bin/env python3
"""Serve only the distribution directory. Never expose the project or local.properties."""
from http.server import ThreadingHTTPServer, SimpleHTTPRequestHandler
from pathlib import Path
from functools import partial

root = Path(__file__).resolve().parent.parent / 'distribution'
class Handler(SimpleHTTPRequestHandler):
    extensions_map = {**SimpleHTTPRequestHandler.extensions_map, '.apk': 'application/vnd.android.package-archive'}
    def do_GET(self):
        if self.path.split('?')[0] not in ('/', '/index.html', '/yingke-camera-0.1.0-beta.4.apk', '/install-qr-0.1.0-beta.4.png', '/version.json', '/SHA256SUMS.txt'):
            self.send_error(404); return
        if self.path.split('?')[0] in ('/yingke-camera-0.1.0-beta.4.apk',) and self.headers.get('Range'):
            import re
            match = re.fullmatch(r'bytes=(\d+)-(\d*)', self.headers['Range'])
            apk = root / self.path.split('?')[0].lstrip('/')
            size = apk.stat().st_size
            if not match:
                self.send_error(416); return
            start = int(match[1]); end = min(int(match[2]) if match[2] else size-1, size-1)
            if not 0 <= start <= end < size:
                self.send_response(416); self.send_header('Content-Range', f'bytes */{size}'); self.end_headers(); return
            self.send_response(206)
            self.send_header('Content-Type', 'application/vnd.android.package-archive')
            self.send_header('Content-Length', str(end-start+1))
            self.send_header('Content-Range', f'bytes {start}-{end}/{size}')
            self.send_header('Accept-Ranges', 'bytes')
            self.end_headers()
            with apk.open('rb') as data:
                data.seek(start); remaining = end-start+1
                while remaining:
                    block = data.read(min(65536, remaining))
                    if not block: break
                    self.wfile.write(block); remaining -= len(block)
            return
        super().do_GET()
    def do_HEAD(self):
        if self.path.split('?')[0] not in ('/', '/index.html', '/yingke-camera-0.1.0-beta.4.apk', '/install-qr-0.1.0-beta.4.png', '/version.json', '/SHA256SUMS.txt'):
            self.send_error(404); return
        super().do_HEAD()
    def end_headers(self):
        self.send_header('Cache-Control', 'no-store')
        super().end_headers()
ThreadingHTTPServer(('0.0.0.0', 8765), partial(Handler, directory=str(root))).serve_forever()
