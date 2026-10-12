#!/usr/bin/env python3
"""Bind the scan-produced CycloneDX document to the pushed image digest."""
import json
from pathlib import Path
import sys
path = Path(sys.argv[1])
document = json.loads(path.read_text())
metadata = document.setdefault("metadata", {})
metadata.setdefault("properties", []).append({"name": "dev-server:image-digest", "value": sys.argv[2]})
path.write_text(json.dumps(document, indent=2) + "\n")
