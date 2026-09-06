import unittest
from wait_for_runtime_release import wait_for_release


class RuntimeReleaseTest(unittest.TestCase):
    def fetch(self, version="2.0.0-rc.49", status="UP"):
        return lambda url: {"status": status} if url.endswith("health") else {"build": {"version": version, "time": "2099-01-01T00:00:00Z"}}

    def test_matches_the_published_version(self):
        result = wait_for_release("https://runtime.example", "2.0.0-rc.49", fetch=self.fetch())
        self.assertEqual("release-ready", result["status"])

    def test_recent_timestamp_does_not_approve_the_wrong_version(self):
        with self.assertRaises(RuntimeError):
            wait_for_release("https://runtime.example", "2.0.0-rc.50", fetch=self.fetch())

    def test_unhealthy_matching_version_is_rejected(self):
        with self.assertRaises(RuntimeError):
            wait_for_release("https://runtime.example", "2.0.0-rc.49", fetch=self.fetch(status="DOWN"))

    def test_rollout_wait_is_bounded(self):
        now = [0]
        def sleep(seconds): now[0] += seconds
        with self.assertRaises(RuntimeError):
            wait_for_release("https://runtime.example", "2.0.0-rc.50", timeout=35,
                             fetch=self.fetch(), clock=lambda: now[0], sleep=sleep)
        self.assertEqual(35, now[0])

    def test_wait_observes_the_new_version(self):
        now = [0]
        def fetch(url):
            return self.fetch("2.0.0-rc.50" if now[0] else "2.0.0-rc.49")(url)
        result = wait_for_release("https://runtime.example", "2.0.0-rc.50", timeout=40,
                                  fetch=fetch, clock=lambda: now[0], sleep=lambda seconds: now.__setitem__(0, now[0] + seconds))
        self.assertEqual("2.0.0-rc.50", result["version"])

    def test_requires_an_explicit_version_and_finite_budget(self):
        for version, timeout in [("", 0), ("latest", 0), ("2.0.0", 601)]:
            with self.assertRaises(ValueError):
                wait_for_release("https://runtime.example", version, timeout, fetch=self.fetch())


if __name__ == "__main__":
    unittest.main()
