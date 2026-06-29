from __future__ import annotations

import unittest

from compare_ab import exact_mcnemar_p_value, parity_checks


class CompareAbTest(unittest.TestCase):
    def test_exact_mcnemar_uses_only_discordant_pairs(self) -> None:
        self.assertAlmostEqual(0.0009765625, exact_mcnemar_p_value(0, 11))
        self.assertEqual(1.0, exact_mcnemar_p_value(0, 0))

    def test_parity_requires_declared_shared_controls_and_full_execution(self) -> None:
        report = {
            "cases": 28,
            "repeat": 3,
            "warmup": 1,
            "request_fingerprint": "request-hash",
            "evaluation_fingerprint": "evaluation-hash",
            "data_snapshot": "soc-events-2026-06-29",
            "provider_config": "gemini-2.5-flash-t0",
            "cache_regime": "cold",
            "summary": {"executed_runs": 84, "total": 84, "execution_target_met": True},
        }

        self.assertTrue(all(passed for _, passed, _ in parity_checks(report, dict(report))))

        candidate = dict(report)
        candidate["data_snapshot"] = "different-snapshot"
        failures = {name for name, passed, _ in parity_checks(report, candidate) if not passed}
        self.assertIn("Data snapshot", failures)


if __name__ == "__main__":
    unittest.main()
