#!/usr/bin/env python3
"""Synthetic guard/exec tests. Never starts Java or contacts MongoDB."""
import contextlib
import io
import json
import os
from pathlib import Path
import runpy
import stat
import sys
import tempfile
import unittest
from unittest.mock import patch

LAUNCHER = Path(__file__).with_name("pocketstats-launch")
ENV = {"MONGODB_URI": "mongodb+srv://fixture:fixture-password@fixture.invalid/",
       "MONGODB_DB": "test", "POCKET_CONSUMER_KEY": "fixture-pocket",
       "READER_ACCESS_TOKEN": "fixture-reader", "SPRING_APPLICATION_JSON": '{"unsafe":true}',
       "JAVA_TOOL_OPTIONS": "fixture-unsafe", "JDK_JAVA_OPTIONS": "fixture-unsafe",
       "_JAVA_OPTIONS": "fixture-unsafe", "SPRING_DATA_MONGODB_AUTO_INDEX_CREATION": "true"}
MARKER = {"version": 2, "database": "test", "mode": "shared-dev-dashboard",
          "source_unit": "pocketstats.service", "source_state": "active-authoritative",
          "evidence_id": "synthetic-local-test"}


class JavaIntercept(Exception):
    pass


class GuardTests(unittest.TestCase):
    def run_guard(self, marker=MARKER, mode=0o640, group=982, check=True, symlink=False):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "marker.json"
            if marker is not None:
                path.write_text(json.dumps(marker))
                path.chmod(mode)
            if symlink:
                link = Path(directory) / "link.json"
                link.symlink_to(path)
                path = link
            real_fstat = os.fstat
            def protected_stat(fd):
                values = list(real_fstat(fd))
                values[4], values[5] = 0, group
                return os.stat_result(values)
            argv = [str(LAUNCHER), "--check-only", "--marker-path", str(path)]
            opened = os.open
            def open_marker(selected, flags, *args):
                self.assertTrue(flags & os.O_NOFOLLOW)
                self.assertTrue(flags & os.O_CLOEXEC)
                return opened(path, flags, *args)
            if not check:
                argv = [str(LAUNCHER)]
            output, errors = io.StringIO(), io.StringIO()
            with patch.dict(os.environ, ENV, clear=True), patch.object(sys, "argv", argv), \
                 patch("os.getgid", return_value=982), patch("os.fstat", side_effect=protected_stat), \
                 patch("os.open", side_effect=open_marker), \
                 patch("os.execve", side_effect=JavaIntercept) as execute, \
                 contextlib.redirect_stdout(output), contextlib.redirect_stderr(errors):
                status = 0
                try:
                    runpy.run_path(str(LAUNCHER), run_name="__main__")
                except SystemExit as error:
                    status = error.code
                except JavaIntercept:
                    status = "exec"
                self.assertFalse(any(value in output.getvalue() + errors.getvalue()
                                     for value in ENV.values()))
                return status, execute.call_args

    def test_valid_gate_does_not_execute_java(self):
        self.assertEqual(self.run_guard(), (0, None))

    def test_missing_old_or_unexpected_marker_rejected_before_java(self):
        for marker in [None, {**MARKER, "version": 1}, {**MARKER, "source_state": "stopped"},
                       {**MARKER, "extra": True}, {**MARKER, "mode": "cutover"}]:
            with self.subTest(marker=marker):
                self.assertEqual(self.run_guard(marker), (78, None))

    def test_writable_wrong_group_or_symlink_marker_rejected(self):
        self.assertEqual(self.run_guard(mode=0o660), (78, None))
        self.assertEqual(self.run_guard(group=983), (78, None))
        self.assertEqual(self.run_guard(symlink=True), (78, None))

    def test_container_exec_preserves_java_and_forces_safe_environment(self):
        status, call = self.run_guard(check=False)
        self.assertEqual(status, "exec")
        executable, argv, environment = call.args
        self.assertEqual(executable, "/usr/bin/java")
        self.assertIn("-Xmx512m", argv)
        self.assertIn("/usr/lib/pocketstats/app.jar", argv)
        self.assertIn("--server.address=0.0.0.0", argv)
        self.assertIn("--server.port=18080", argv)
        self.assertIn("--server.servlet.context-path=/pocketstats", argv)
        self.assertIn("--spring.data.mongodb.auto-index-creation=false", argv)
        self.assertEqual(environment["SPRING_DATA_MONGODB_AUTO_INDEX_CREATION"], "false")
        self.assertEqual(environment["SPRING_DATA_MONGODB_URI"], ENV["MONGODB_URI"])
        for key in ["SPRING_APPLICATION_JSON", "JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS"]:
            self.assertNotIn(key, environment)


if __name__ == "__main__":
    unittest.main(verbosity=2)
