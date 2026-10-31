#!/usr/bin/env python3
"""Runner tests without calls to Readwise or a database."""
import importlib.util
import io
import json
from pathlib import Path
import unittest
from unittest.mock import patch
from urllib.error import URLError

spec = importlib.util.spec_from_file_location(
    "readwise_sync", Path(__file__).with_name("readwise-sync.py"))
runner = importlib.util.module_from_spec(spec)
spec.loader.exec_module(runner)


class ReadwiseSyncTest(unittest.TestCase):
    def response(self, payload):
        return io.BytesIO(json.dumps(payload).encode())

    def test_available_source_imports_once_with_required_header(self):
        with patch.object(runner.OPENER, "open", side_effect=[
            self.response({"status": 0, "data": {"available": True}}),
            self.response({"status": 0, "data": {
                "success": True, "records": 21, "date": "2026-10-31T09:00:00Z"}}),
        ]) as request:
            self.assertEqual(runner.main(), 0)
        self.assertEqual(request.call_count, 2)
        post = request.call_args_list[1].args[0]
        self.assertEqual(post.full_url, runner.BASE_URL + "/sync/last/READER")
        self.assertEqual(post.get_method(), "POST")
        self.assertEqual(post.get_header("X-pocketstats-sync"), "1")

    def test_unconfigured_source_never_submits_import(self):
        with patch.object(runner.OPENER, "open", return_value=self.response(
                {"status": 0, "data": {"available": False}})) as request:
            self.assertEqual(runner.main(), 1)
            self.assertEqual(request.call_count, 1)

    def test_application_failure_is_not_success(self):
        with patch.object(runner.OPENER, "open", side_effect=[
            self.response({"status": 0, "data": {"available": True}}),
            self.response({"status": 0, "data": {"success": False}}),
        ]):
            self.assertEqual(runner.main(), 1)

    def test_invalid_json_is_failure(self):
        with patch.object(runner.OPENER, "open", return_value=io.BytesIO(b"invalid")):
            self.assertEqual(runner.main(), 1)

    def test_post_timeout_is_not_retried(self):
        with patch.object(runner.OPENER, "open", side_effect=[
            self.response({"status": 0, "data": {"available": True}}),
            TimeoutError(),
        ]) as request:
            self.assertEqual(runner.main(), 1)
            self.assertEqual(request.call_count, 2)

    def test_unreachable_service_is_failure(self):
        with patch.object(runner.OPENER, "open", side_effect=URLError("unreachable")):
            self.assertEqual(runner.main(), 1)


if __name__ == "__main__":
    unittest.main()
