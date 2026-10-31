#!/usr/bin/env python3
"""Run one incremental Readwise import through the private local proxy."""
import json
import sys
from urllib.error import HTTPError, URLError
from urllib.request import ProxyHandler, Request, build_opener

BASE_URL = "http://127.0.0.1:18081"
# Ignore host proxy environment variables for this loopback-only operation.
OPENER = build_opener(ProxyHandler({}))


def request(path, *, method="GET", timeout=15):
    req = Request(BASE_URL + path, method=method,
                  data=b"" if method == "POST" else None,
                  headers={"Accept": "application/json", "X-PocketStats-Sync": "1"})
    with OPENER.open(req, timeout=timeout) as response:
        payload = json.load(response)
    if not isinstance(payload, dict) or payload.get("status") != 0:
        raise ValueError("API reported failure")
    data = payload.get("data")
    if not isinstance(data, dict):
        raise ValueError("API returned invalid data")
    return data


def main():
    try:
        if request("/sync/availability").get("available") is not True:
            raise ValueError("Readwise synchronization is not configured")
        print("Starting scheduled Readwise synchronization", flush=True)
        # No automatic retry: after a timeout the import may still be running.
        result = request("/sync/last/READER", method="POST", timeout=310)
        if (result.get("success") is not True
                or type(result.get("records")) is not int
                or result["records"] < 0
                or not isinstance(result.get("date"), str)
                or not result["date"]):
            raise ValueError("Readwise import did not report a successful result")
        print(f"Readwise synchronization succeeded: {result['records']} records, "
              f"date={result['date']}", flush=True)
        return 0
    except HTTPError as error:
        print(f"Readwise synchronization failed: HTTP {error.code}", file=sys.stderr)
    except (URLError, OSError, ValueError) as error:
        # Do not include response bodies or transport details in the journal.
        print(f"Readwise synchronization failed ({type(error).__name__})", file=sys.stderr)
    return 1


if __name__ == "__main__":
    sys.exit(main())
