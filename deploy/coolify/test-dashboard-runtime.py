#!/usr/bin/env python3
"""Test runtime configuration in the pinned proxy image with public UI files only.

Run on the trusted Docker host with the public UI release path as argument.
No host ports, backend values, application JAR or Docker socket are mounted.
"""
import json
from pathlib import Path
import subprocess
import sys
import time
import unittest
import uuid

ROOT = Path(__file__).resolve().parent
IMAGE = "sha256:0b25a392c599559c0c18c88c37c777f4aa7bf5358ed9217928d1dc99ce056f49"
UI = Path(sys.argv.pop(1)).resolve()


def command(args, **kwargs):
    return subprocess.run(args, capture_output=True, text=True, timeout=15, **kwargs)


class DashboardRuntimeTests(unittest.TestCase):
    def fixture(self, value, detached=True):
        name = "pocketstats-dashboard-env-fixture-" + uuid.uuid4().hex
        self.addCleanup(lambda: command(["docker", "rm", "-f", name]))
        args = ["docker", "run", "--name", name, "--pull", "never",
                "--network", "none", "--add-host", "pocketstats:127.0.0.1",
                "--user", "101:101", "--read-only", "--cap-drop", "ALL",
                "--security-opt", "no-new-privileges:true", "--memory", "64m",
                "--memory-swap", "64m", "--cpus", "0.1", "--pids-limit", "48",
                "--log-driver", "none", "--tmpfs", "/tmp:rw,size=8m,mode=1777",
                "--env", "VUE_APP_BACKEND_URL=" + value,
                "--mount", "type=bind,src=" + str(UI) + ",dst=/usr/share/nginx/html,readonly",
                "--mount", "type=bind,src=" + str(ROOT / "nginx.conf") + ",dst=/etc/nginx/nginx.conf,readonly",
                "--mount", "type=bind,src=" + str(ROOT / "dashboard-start") + ",dst=/usr/local/libexec/dashboard-start,readonly",
                "--entrypoint", "/bin/sh"]
        if detached:
            args.append("-d")
        result = command(args + [IMAGE, "/usr/local/libexec/dashboard-start"])
        if not detached:
            return result
        self.assertEqual(result.returncode, 0, "fixture creation failed")
        for _ in range(30):
            response = command(["docker", "exec", name, "wget", "-q", "-O", "-", "http://127.0.0.1:8080/runtime-config.js"])
            if response.returncode == 0:
                return name, response.stdout
            time.sleep(0.1)
        self.fail("runtime configuration did not become ready")

    def test_runtime_api_base_changes_without_rebuilding_the_image(self):
        for value in ("", "/api", "https://fixture.invalid/pocketstats/"):
            with self.subTest(value=value):
                name, source = self.fixture(value)
                self.assertTrue(source.startswith("window.POCKETSTATS_CONFIG="))
                actual = json.loads(source.removeprefix("window.POCKETSTATS_CONFIG=").strip().removesuffix(";"))
                self.assertEqual(actual, {"backendUrl": value.removesuffix("/")})
                index = command(["docker", "exec", name, "wget", "-q", "-O", "-", "http://127.0.0.1:8080/"])
                self.assertEqual(index.returncode, 0)
                self.assertIn('<script src="/runtime-config.js"></script>', index.stdout)
                headers = command(["docker", "exec", name, "wget", "-S", "-O", "/dev/null", "http://127.0.0.1:8080/"])
                self.assertEqual(headers.returncode, 0)
                self.assertIn("Content-Type: text/html", headers.stderr)
                headers = command(["docker", "exec", name, "wget", "-S", "-O", "/dev/null", "http://127.0.0.1:8080/runtime-config.js"])
                self.assertEqual(headers.returncode, 0)
                self.assertIn("Content-Type: application/javascript", headers.stderr)
                bundle = command(["docker", "exec", name, "wget", "-q", "-O", "-", "http://127.0.0.1:8080/js/app.779754b1.js"])
                self.assertEqual(bundle.returncode, 0)
                self.assertIn("backendUrl(){return window.POCKETSTATS_CONFIG.backendUrl}", bundle.stdout)
                metadata = json.loads(command(["docker", "inspect", name]).stdout)[0]
                self.assertEqual(metadata["HostConfig"]["NetworkMode"], "none")
                self.assertFalse(metadata["HostConfig"].get("PortBindings"))
                self.assertEqual(metadata["Config"]["User"], "101:101")

    def test_script_injection_invalid_schemes_and_multiline_values_are_rejected(self):
        for value in ('https://fixture.invalid/";alert(1)//', "javascript:alert(1)", "//fixture.invalid", "/api\n/other", "/api\\quote", "/api?query=x", "/api#fragment"):
            with self.subTest(value=value):
                result = self.fixture(value, detached=False)
                self.assertEqual(result.returncode, 78)
                self.assertIn("configuration rejected", result.stderr)


if __name__ == "__main__":
    unittest.main(verbosity=2)
