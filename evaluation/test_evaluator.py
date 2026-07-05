from __future__ import annotations

import json
import unittest
from pathlib import Path
from unittest.mock import patch

from evaluator import EvaluationCase, is_aggregation_case, load_cases, run_case
from generate_v3_cases import derive, load_eda, load_source
from metrics import summarize_runs
from runner import DEFAULT_CASES, benchmark_passed, cases_fingerprint, parse_args


class V3BenchmarkTest(unittest.TestCase):
    def test_runner_defaults_to_v3_direct_workflow(self) -> None:
        with patch("sys.argv", ["runner.py"]):
            args = parse_args()
        self.assertEqual([Path("evaluation/cases/v3_cases.jsonl")], DEFAULT_CASES)
        self.assertEqual(DEFAULT_CASES, args.cases)
        self.assertEqual("workflow-v3", args.variant)
        self.assertEqual(120.0, args.timeout)
        self.assertEqual(28, args.require_executions)
        self.assertEqual(Path("evaluation/reports/v3_report.md"), args.output)
        self.assertEqual(Path("evaluation/reports/v3_report.json"), args.json_output)
        self.assertFalse(hasattr(args, "auto_confirm"))

    def test_v3_is_distinct_and_contains_no_assisted_protocol(self) -> None:
        source = load_source(Path("evaluation/cases/llm_cases.jsonl"))
        v3 = derive(Path("evaluation/cases/llm_cases.jsonl"), source, load_eda(Path("evaluation/eda/v3_case_adaptations.json")))
        self.assertEqual(28, len(v3))
        self.assertEqual([case["id"] for case in source], [case["id"] for case in v3])
        self.assertTrue(all("assisted" not in case for case in v3))
        self.assertTrue(all(case["executionRequest"].get("sessionId", "").startswith("benchmark-v3-") for case in v3))
        self.assertEqual(source[8]["request"], v3[8]["request"])
        self.assertNotEqual(v3[8]["request"], v3[8]["executionRequest"])
        self.assertIn("event_type", v3[8]["executionRequest"]["question"])

    def test_v3_file_matches_generator(self) -> None:
        generated = derive(
            Path("evaluation/cases/llm_cases.jsonl"),
            load_source(Path("evaluation/cases/llm_cases.jsonl")),
            load_eda(Path("evaluation/eda/v3_case_adaptations.json")),
        )
        actual = [json.loads(line) for line in Path("evaluation/cases/v3_cases.jsonl").read_text(encoding="utf-8").splitlines()]
        self.assertEqual(generated, actual)

    def test_direct_execution_is_scored_without_confirmation(self) -> None:
        case = EvaluationCase(
            source="v3.jsonl:1", id="v3-test", category="test", description="", tags=[],
            request={"question": "show events", "sessionId": "benchmark-v3-test"},
            execution_request={"question": "show events", "sessionId": "benchmark-v3-test"},
            expected={"status": 200, "selectedTemplate": "IQL", "allowZeroResults": True},
            thresholds={},
        )
        response = {
            "selectedTemplate": "IQL", "generatedDsl": {"query": {"match_all": {}}},
            "totalCount": 0, "results": [], "aggregations": [], "cacheHit": False,
        }
        with patch("evaluator._post_json", return_value={
            "status": 200, "latency_ms": 10.0, "response_json": response,
            "response_text": json.dumps(response), "error": "",
        }) as post:
            result = run_case(case, "http://localhost:8080", 10.0, 1)
        self.assertTrue(result["passed"])
        self.assertTrue(result["final_execution"])
        self.assertEqual("http://localhost:8080/api/search", post.call_args.args[0])
        summary = summarize_runs([result])
        self.assertNotIn("confirmation_runs", summary)

    def test_metric_denominator_stays_ground_truth_based(self) -> None:
        cases = load_cases([Path("evaluation/cases/v3_cases.jsonl")])
        self.assertEqual(11, sum(1 for case in cases if is_aggregation_case(case)))
        self.assertEqual(17, sum(1 for case in cases if not is_aggregation_case(case)))

    def test_v1_and_v3_have_identical_comparison_fingerprints(self) -> None:
        v1 = load_cases([Path("evaluation/cases/llm_cases.jsonl")])
        v3 = load_cases([Path("evaluation/cases/v3_cases.jsonl")])
        self.assertEqual(cases_fingerprint(v1, False), cases_fingerprint(v3, False))
        self.assertEqual(cases_fingerprint(v1, True), cases_fingerprint(v3, True))

    def test_execution_target_affects_verdict(self) -> None:
        self.assertFalse(benchmark_passed({"pass_rate": 1.0, "execution_target_met": False}, 0.9))
        self.assertTrue(benchmark_passed({"pass_rate": 1.0, "execution_target_met": True}, 0.9))


if __name__ == "__main__":
    unittest.main()
