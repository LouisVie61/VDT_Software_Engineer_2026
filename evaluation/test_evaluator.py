from __future__ import annotations

import json
import unittest
from pathlib import Path
from unittest.mock import patch

from evaluator import EvaluationCase, assisted_evaluation_case, load_cases, run_case
from metrics import summarize_runs
from runner import benchmark_passed


class AssistedExecutionTest(unittest.TestCase):
    def test_auto_confirm_merges_case_override_and_counts_final_execution(self) -> None:
        case = EvaluationCase(
            source="test.jsonl:1",
            id="amb-003",
            category="ambiguity",
            description="test",
            tags=[],
            request={"question": "thong ke cac loi", "page": 0, "pageSize": 50},
            expected={
                "status": 200,
                "needsConfirmation": True,
                "nonEmptyFields": ["confirmation.confirmationId"],
                "allowZeroResults": True,
            },
            thresholds={},
            assisted={
                "editedIntent": {
                    "intent": "TERMS_AGGREGATION",
                    "groupBy": "event_type",
                    "topN": 10,
                }
            },
        )
        initial_response = {
            "needsConfirmation": True,
            "totalCount": 0,
            "results": [],
            "aggregations": [],
            "confirmation": {
                "confirmationId": "confirmation-1",
                "intent": {"intent": "TERMS_AGGREGATION", "metric": "COUNT"},
            },
        }
        final_response = {
            "needsConfirmation": False,
            "selectedTemplate": "TERMS_AGGREGATION",
            "generatedDsl": {
                "query": {"match_all": {}},
                "sort": [{"timestamp": {"order": "desc"}}],
            },
            "totalCount": 0,
            "results": [],
            "aggregations": [],
            "cacheHit": False,
        }
        calls: list[tuple[str, dict]] = []

        def fake_post(url: str, payload: dict, timeout: float) -> dict:
            calls.append((url, payload))
            response = initial_response if len(calls) == 1 else final_response
            return {
                "status": 200,
                "latency_ms": 10.0,
                "response_json": response,
                "response_text": json.dumps(response),
                "error": "",
            }

        with patch("evaluator._post_json", side_effect=fake_post):
            result = run_case(case, "http://localhost:8080", 10.0, 1, auto_confirm=True)

        self.assertTrue(result["passed"])
        self.assertTrue(result["initial_needs_confirmation"])
        self.assertTrue(result["confirmation_followed"])
        self.assertTrue(result["final_execution"])
        self.assertEqual(20.0, result["latency_ms"])
        self.assertEqual("http://localhost:8080/api/search/confirm", calls[1][0])
        edited_intent = calls[1][1]["editedIntent"]
        self.assertEqual("COUNT", edited_intent["metric"])
        self.assertEqual("event_type", edited_intent["groupBy"])
        self.assertEqual(10, edited_intent["topN"])

        summary = summarize_runs([result])
        self.assertEqual(1, summary["executed_runs"])
        self.assertEqual(1, summary["confirmation_runs"])
        self.assertEqual(1, summary["confirmation_followed_runs"])

    def test_workflow_dataset_has_assisted_overrides_for_incomplete_intents(self) -> None:
        cases = load_cases([Path("evaluation/cases/ablation/workflow_comparison_cases.jsonl")])
        by_id = {case.id: case for case in cases}

        self.assertEqual(28, len(cases))
        self.assertEqual("event_type", by_id["amb-003"].assisted["editedIntent"]["groupBy"])
        self.assertTrue(by_id["amb-003"].assisted["initialExpected"]["needsConfirmation"])
        self.assertEqual(10, by_id["amb-007"].assisted["editedIntent"]["topN"])
        self.assertEqual("event_type", by_id["amb-008"].assisted["editedIntent"]["groupBy"])
        self.assertEqual("TIME_AGGREGATION", by_id["llm-003"].assisted["editedIntent"]["intent"])

    def test_ab_dataset_uses_shared_workflow_payload_and_final_scoring(self) -> None:
        cases = load_cases([Path("evaluation/cases/ablation/ab_execution_cases.jsonl")])
        workflow_cases = load_cases([Path("evaluation/cases/ablation/workflow_comparison_cases.jsonl")])
        workflow_requests = {case.id: case.request for case in workflow_cases}

        self.assertEqual(28, len(cases))
        self.assertEqual(28, len({case.id for case in cases}))
        self.assertTrue(all(case.request == workflow_requests[case.id] for case in cases))
        self.assertTrue(all(case.assisted.get("scoreFinalResponse") is True for case in cases))

    def test_benchmark_fails_when_execution_target_is_not_met(self) -> None:
        self.assertFalse(benchmark_passed({"pass_rate": 1.0, "execution_target_met": False}, 0.9))
        self.assertTrue(benchmark_passed({"pass_rate": 1.0, "execution_target_met": True}, 0.9))

    def test_assisted_flow_uses_end_to_end_latency_budget(self) -> None:
        case = EvaluationCase(
            source="test.jsonl:1",
            id="case-1",
            category="test",
            description="test",
            tags=[],
            request={"question": "test"},
            expected={"status": 200},
            thresholds={"max_latency_ms": 3000},
            assisted={"initialExpected": {"status": 200, "needsConfirmation": True}},
        )

        assisted = assisted_evaluation_case(case, confirmation_followed=True)

        self.assertEqual(6000.0, assisted.thresholds["max_latency_ms"])
        self.assertTrue(assisted.expected["needsConfirmation"])

    def test_score_final_response_checks_semantics_after_confirmation(self) -> None:
        case = EvaluationCase(
            source="ab.jsonl:1",
            id="agg-1",
            category="ab",
            description="test",
            tags=[],
            request={"question": "top users"},
            expected={
                "status": 200,
                "needsConfirmation": False,
                "selectedTemplate": "TERMS_AGGREGATION",
                "generatedDslContains": ['"field":"user"'],
                "generatedDslPath": {"exists": ["query", "aggs"]},
                "allowZeroResults": True,
            },
            thresholds={},
            assisted={"scoreFinalResponse": True},
        )
        initial_response = {
            "needsConfirmation": True,
            "confirmation": {
                "confirmationId": "confirmation-1",
                "intent": {"intent": "TERMS_AGGREGATION"},
            },
        }
        final_response = {
            "needsConfirmation": False,
            "selectedTemplate": "TERMS_AGGREGATION",
            "generatedDsl": {
                "query": {"match_all": {}},
                "aggs": {"top_values": {"terms": {"field": "source"}}},
            },
            "totalCount": 1,
            "results": [{}],
            "aggregations": [{}],
        }

        responses = [initial_response, final_response]

        def fake_post(url: str, payload: dict, timeout: float) -> dict:
            response = responses.pop(0)
            return {
                "status": 200,
                "latency_ms": 10.0,
                "response_json": response,
                "response_text": json.dumps(response),
                "error": "",
            }

        with patch("evaluator._post_json", side_effect=fake_post):
            result = run_case(case, "http://localhost:8080", 10.0, 1, auto_confirm=True)

        self.assertFalse(result["passed"])
        failed_names = {check.name for check in result["checks"] if not check.passed}
        self.assertIn('final:dsl_contains:"field":"user"', failed_names)
        self.assertTrue(result["final_execution"])


if __name__ == "__main__":
    unittest.main()
