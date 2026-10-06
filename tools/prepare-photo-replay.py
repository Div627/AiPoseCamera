#!/usr/bin/env python3
"""Prepare ignored public fixtures for ReplayFixturesTest (requires curl and ffmpeg)."""
import hashlib
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / '.local-validation'
SOURCES = {
    'portrait': 'https://storage.googleapis.com/mediapipe-assets/portrait.jpg',
    'pose': 'https://storage.googleapis.com/mediapipe-assets/pose.jpg',
    'mountain': 'https://thumb.wikimedia.org/wikipedia/commons/thumb/c/c9/CH.VS.Zermatt_2021-10-17_Matterhorn_8726.jpg/1280px-CH.VS.Zermatt_2021-10-17_Matterhorn_8726.jpg',
}
ROOT.mkdir(exist_ok=True)
for name, url in SOURCES.items():
    image = ROOT / f'{name}.jpg'
    if not image.exists():
        subprocess.run(['curl', '--fail', '--location', '--retry', '2', url, '-o', str(image)], check=True)
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', str(image), '-frames:v', '1', str(ROOT / f'{name}.ppm')], check=True)
    print(name, hashlib.sha256(image.read_bytes()).hexdigest())
print('Fixtures prepared. Attribution and replay limitations: docs/validation-beta.9.md')
