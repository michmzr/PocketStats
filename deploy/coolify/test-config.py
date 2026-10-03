#!/usr/bin/env python3
"""Offline parsed Compose and exact proxy-contract checks; no deployment."""
import json
from pathlib import Path
import re
import subprocess
import unittest

ROOT = Path(__file__).resolve().parent


class ConfigurationTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        result = subprocess.run(
            ["ruby", "--disable-gems", "-ryaml", "-rjson", "-e",
             "puts JSON.generate(YAML.safe_load(File.read(ARGV[0]), permitted_classes: [], permitted_symbols: [], aliases: true))",
             str(ROOT / "compose.yaml")], text=True, capture_output=True, check=True,
        )
        cls.config = json.loads(result.stdout)
        cls.services = cls.config["services"]
        cls.proxy = (ROOT / "nginx.conf").read_text()

    def test_only_supported_dashboard_port_is_published(self):
        self.assertNotIn("ports", self.services["pocketstats"])
        self.assertEqual(self.services["dashboard"]["ports"], ["127.0.0.1:18081:8080"])
        for service in self.services.values():
            self.assertNotIn("labels", service)
            self.assertNotIn("network_mode", service)
            self.assertEqual(service["pull_policy"], "never")
            self.assertTrue(service["image"].startswith("${POCKETSTATS_"))
            self.assertIn(":?", service["image"])

    def test_numeric_identity_caps_guard_mounts_and_raw_environment(self):
        backend = self.services["pocketstats"]
        self.assertEqual(backend["user"], "999:982")
        self.assertEqual(backend["mem_limit"], "768m")
        self.assertEqual(backend["memswap_limit"], "768m")
        self.assertEqual(backend["cpus"], "1")
        self.assertEqual(backend["entrypoint"], ["/usr/bin/python3", "-I", "/usr/local/libexec/pocketstats-launch"])
        mounts = {mount["target"]: mount for mount in backend["volumes"]}
        for path in ["/usr/lib/pocketstats/app.jar", "/etc/pocketstats-dashboard.json", "/usr/local/libexec/pocketstats-launch"]:
            self.assertTrue(mounts[path]["read_only"])
            self.assertFalse(mounts[path]["bind"]["create_host_path"])
        self.assertEqual(backend["env_file"], [{"path": "/etc/pocketstats/pocketstats-compose.env", "required": True, "format": "raw"}])

    def test_dedicated_proxy_ingress_and_no_shared_coolify_network(self):
        self.assertEqual(self.config["networks"], {
            "dashboard": {"internal": True}, "atlas_egress": {},
            "proxy_ingress": {"driver": "bridge", "internal": False,
                "driver_opts": {"com.docker.network.bridge.name": "ps-ui-ingress"}},
        })
        self.assertEqual(self.services["pocketstats"]["networks"], ["dashboard", "atlas_egress"])
        self.assertEqual(self.services["dashboard"]["networks"], ["dashboard", "proxy_ingress"])
        self.assertNotIn("coolify", json.dumps(self.config["networks"]))

    def test_no_automatic_restart_or_unbounded_docker_logs(self):
        for service in self.services.values():
            self.assertEqual(service["restart"], "no")
            self.assertTrue(service["read_only"])
            self.assertEqual(service["cap_drop"], ["ALL"])
            self.assertEqual(service["logging"], {"driver": "local", "options": {"max-size": "5m", "max-file": "3"}})

    def test_proxy_exact_statistics_methods_and_default_denial(self):
        expected = {"langs": "GET", "byPeriods": "GET", "topTags": "GET", "heatmap": "GET", "byDay": "POST"}
        self.assertEqual(self.proxy.count("proxy_pass "), len(expected))
        for name, method in expected.items():
            start = self.proxy.index("location = /stats/" + name + " {")
            next_location = self.proxy.find("location ", start + 1)
            block = self.proxy[start:next_location if next_location >= 0 else len(self.proxy)]
            self.assertIn("limit_except " + method + " { deny all; }", block)
            self.assertIn("proxy_pass http://pocketstats:18080/pocketstats/stats/" + name + ";", block)
            self.assertIn('proxy_set_header Cookie "";', block)
            self.assertIn('proxy_set_header Authorization "";', block)
        self.assertRegex(self.proxy, r"location /\s*\{\s*return 403;\s*\}")
        self.assertIn("client_max_body_size 8k;", self.proxy)
        self.assertIn("access_log off;", self.proxy)


if __name__ == "__main__":
    unittest.main(verbosity=2)
