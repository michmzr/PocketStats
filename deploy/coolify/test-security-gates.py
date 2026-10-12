#!/usr/bin/env python3
"""Exercise scanner reporting with synthetic findings and no real credentials."""
import json
from pathlib import Path
import subprocess
import tempfile
import unittest
CHECK = Path(__file__).with_name("check-scan.py")
class SecurityGateTests(unittest.TestCase):
    def check(self, finding):
        with tempfile.TemporaryDirectory() as temp:
            report = Path(temp) / "report.json"
            report.write_text(json.dumps({"Results": [finding]}))
            return subprocess.run(["python3", str(CHECK), str(report)], capture_output=True, text=True)
    def test_fixed_high_vulnerability_blocks_but_unfixed_does_not(self):
        item = {"VulnerabilityID": "fixture-CVE", "PkgName": "fixture", "InstalledVersion": "1", "FixedVersion": "2", "Severity": "HIGH"}
        self.assertEqual(self.check({"Vulnerabilities": [item]}).returncode, 1)
        self.assertEqual(self.check({"Vulnerabilities": [{**item, "FixedVersion": ""}]}).returncode, 0)
    def test_secret_values_never_appear_in_output(self):
        result = self.check({"Target": "fixture", "Secrets": [{"RuleID": "fixture", "Match": "synthetic-private-value"}]})
        self.assertEqual(result.returncode, 1)
        self.assertNotIn("synthetic-private-value", result.stdout + result.stderr)
    def test_invalid_high_configuration_blocks(self):
        self.assertEqual(self.check({"Misconfigurations": [{"ID": "fixture", "Status": "FAIL", "Severity": "HIGH"}]}).returncode, 1)
if __name__ == "__main__":
    unittest.main(verbosity=2)
