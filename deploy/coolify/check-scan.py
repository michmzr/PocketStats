#!/usr/bin/env python3
"""Report finding identities only; never echo secret match values."""
import json
import sys
report = json.load(open(sys.argv[1]))
blocked = False
for result in report.get("Results", []):
    for item in result.get("Vulnerabilities", []):
        if item.get("Severity") in ("HIGH", "CRITICAL") and item.get("FixedVersion"):
            print(item["VulnerabilityID"], item["PkgName"], item["InstalledVersion"], "fixed:", item["FixedVersion"])
            blocked = True
    for item in result.get("Secrets", []):
        print("Secret pattern rejected:", item.get("RuleID", "unknown"), "in", result.get("Target", "unknown"))
        blocked = True
    for item in result.get("Misconfigurations", []):
        if item.get("Severity") in ("HIGH", "CRITICAL") and item.get("Status") == "FAIL":
            print("Configuration rejected:", item.get("ID"), result.get("Target"))
            blocked = True
raise SystemExit(1 if blocked else 0)
