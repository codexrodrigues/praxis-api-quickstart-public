"""Observe an already-published release using the existing actuator build.version."""
import argparse
import json
import re
import time
import urllib.request
from urllib.parse import urlsplit


def read_json(url):
    with urllib.request.urlopen(url, timeout=10) as response:
        return json.load(response)


def wait_for_release(base_url, expected_version, timeout=0, fetch=read_json,
                     clock=time.monotonic, sleep=time.sleep):
    parts = urlsplit(base_url)
    if parts.scheme not in ("https", "http") or not parts.netloc or parts.username or parts.query or parts.fragment:
        raise ValueError("An HTTP(S) runtime base URL without credentials/query is required")
    if not re.fullmatch(r"\d+\.\d+\.\d+(?:[-+][0-9A-Za-z.-]+)?", expected_version):
        raise ValueError("An explicit release version is required")
    if not 0 <= timeout <= 600:
        raise ValueError("Rollout observation timeout must be between 0 and 600 seconds")
    deadline = clock() + timeout
    observed = None
    while True:
        try:
            health = fetch(base_url.rstrip("/") + "/actuator/health")
            info = fetch(base_url.rstrip("/") + "/actuator/info")
            observed = info.get("build", {}).get("version")
            if health.get("status") == "UP" and observed == expected_version:
                return {"status": "release-ready", "version": observed}
        except (OSError, ValueError, TypeError, AttributeError):
            observed = None
        remaining = deadline - clock()
        if remaining <= 0:
            raise RuntimeError(f"Healthy runtime version {expected_version} was not observed (last version: {observed!r})")
        sleep(min(20, remaining))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--backend-url", required=True)
    parser.add_argument("--expected-version", required=True)
    parser.add_argument("--timeout", type=int, default=0)
    args = parser.parse_args()
    print(json.dumps(wait_for_release(args.backend_url, args.expected_version, args.timeout)))
