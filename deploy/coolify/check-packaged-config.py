#!/usr/bin/env python3
"""Reject credentials embedded in the newly built source JAR, without logging values."""
from pathlib import Path
import re
import zipfile

for source in Path("src/main/java").rglob("*.java"):
    assert not re.search(r'(?i)(?:accessToken|consumerKey|readerAccessToken)\s*=\s*"[^"\s]+"', source.read_text()), "hardcoded source credential rejected"

jar = Path("build/libs/app.jar")
with zipfile.ZipFile(jar) as archive:
    resources = {n: archive.read(n).decode() for n in archive.namelist()
                 if n.startswith("BOOT-INF/classes/application") and n.endswith((".yaml", ".yml", ".properties"))}
    assert resources, "application resources missing"
    assert "BOOT-INF/classes/static/index.html" in archive.namelist(), "built frontend missing"
    base = resources["BOOT-INF/classes/application.yaml"]
    assert "uri: ${MONGODB_URI}" in base, "MongoDB must bind only from runtime"
    assert "consumer-key: ${POCKET_CONSUMER_KEY}" in base, "Pocket must bind only from runtime"
    owned = [archive.read(n) for n in archive.namelist()
             if n.startswith("BOOT-INF/classes/") and not n.startswith("BOOT-INF/classes/static/")]
    assert not any(re.search(rb"mongodb(?:\+srv)?://[^\s\x00]+@", content) for content in owned), "embedded database credential rejected"
    for text in resources.values():
        assert not re.search(r"mongodb(?:\+srv)?://[^\s]+@", text), "embedded database credential rejected"
        assert not re.search(r"(?im)^\s*(?:password|reader_access_token|consumer-key):\s*(?!\$\{)\S", text), "embedded credential rejected"
print("New source JAR runtime credential bindings verified")
