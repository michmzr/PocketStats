#!/usr/bin/env python3
"""Keep vulnerability identities and secret counts, never secret match values."""
import json
from datetime import date, timezone, datetime
from pathlib import Path
import sys

report = json.loads(Path(sys.argv[1]).read_text())
findings = [v for result in report.get('Results', [])
            for v in result.get('Vulnerabilities', [])
            if v.get('Severity') in ('HIGH', 'CRITICAL') and v.get('FixedVersion')]
secrets = sum(len(result.get('Secrets', [])) for result in report.get('Results', []))
accept = len(sys.argv) == 4 and sys.argv[3] == '--owner-exception-20261014'
if accept and datetime.now(timezone.utc).date() > date(2026, 10, 21):
    raise SystemExit('Owner vulnerability exception expired')
summary = {
    'ownerVulnerabilityException': '2026-10-14 private deployment' if accept else None,
    'artifact': report.get('ArtifactName'),
    'sourceRevision': report.get('Metadata', {}).get('ImageConfig', {}).get('config', {}).get('Labels', {}).get('org.opencontainers.image.revision'),
    'secretMatches': secrets,
    'fixableHighCritical': [{k: v.get(k) for k in
        ('VulnerabilityID', 'PkgName', 'InstalledVersion', 'FixedVersion', 'Severity')}
        for v in findings],
}
Path(sys.argv[2]).write_text(json.dumps(summary, indent=2) + '\n')
print(json.dumps({'fixableHighCritical': len(findings), 'secretMatches': secrets}))
raise SystemExit(1 if secrets or (findings and not accept) else 0)
