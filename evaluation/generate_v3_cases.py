from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any


DEFAULT_SOURCE = Path("evaluation/cases/llm_cases.jsonl")
DEFAULT_OUTPUT = Path("evaluation/cases/v3_cases.jsonl")
DEFAULT_EDA = Path("evaluation/eda/v3_case_adaptations.json")
DERIVATION_VERSION = "v3-direct-iql-workflow-1"


def canonical_json(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"))


def sha256(value: Any) -> str:
    return hashlib.sha256(canonical_json(value).encode("utf-8")).hexdigest()


def load_source(path: Path) -> list[dict[str, Any]]:
    cases: list[dict[str, Any]] = []
    with path.open("r", encoding="utf-8-sig") as handle:
        for line_number, line in enumerate(handle, start=1):
            if not line.strip():
                continue
            case = json.loads(line)
            for field in ("id", "category", "request", "expected"):
                if field not in case:
                    raise ValueError(f"{path}:{line_number}: missing {field}")
            cases.append(case)
    if not cases:
        raise ValueError(f"{path}: source is empty")
    if len({case["id"] for case in cases}) != len(cases):
        raise ValueError(f"{path}: duplicate case id")
    return cases


def load_eda(path: Path) -> dict[str, Any]:
    profile = json.loads(path.read_text(encoding="utf-8"))
    if profile.get("version") != "v3-eda-1" or not isinstance(profile.get("caseAdaptations"), dict):
        raise ValueError(f"{path}: unsupported EDA profile")
    return profile


def derive(source: Path, cases: list[dict[str, Any]], eda: dict[str, Any]) -> list[dict[str, Any]]:
    source_hash = sha256(cases)
    derived: list[dict[str, Any]] = []
    for source_case in cases:
        source_id = source_case["id"]
        request = json.loads(json.dumps(source_case["request"]))
        execution_request = json.loads(json.dumps(request))
        adaptation = eda["caseAdaptations"].get(source_id, {})
        if adaptation.get("question"):
            execution_request["question"] = adaptation["question"]
        execution_request.update(adaptation.get("requestOverrides", {}))
        execution_request["sessionId"] = f"benchmark-v3-{source_id}"

        derived.append({
            "id": source_id,
            "category": source_case["category"],
            "tags": [*source_case.get("tags", []), "v3", "direct_iql"],
            "description": f"Direct IQL workflow case derived from {source_id}",
            "request": request,
            "executionRequest": execution_request,
            "expected": json.loads(json.dumps(source_case["expected"])),
            "thresholds": source_case.get("thresholds", {}),
            "provenance": {
                "source": source.name,
                "sourceCaseId": source_id,
                "sourceCaseSha256": sha256(source_case),
                "sourceWorkloadSha256": source_hash,
                "edaProfileSha256": sha256(eda),
                "edaRationale": adaptation.get("rationale", "No request rewrite required by EDA."),
                "derivation": DERIVATION_VERSION,
            },
        })
    return derived


def main() -> int:
    parser = argparse.ArgumentParser(description="Derive the V3 direct-IQL workload from llm_cases.jsonl.")
    parser.add_argument("--source", type=Path, default=DEFAULT_SOURCE)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument("--eda", type=Path, default=DEFAULT_EDA)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()

    rendered = "".join(canonical_json(case) + "\n" for case in derive(args.source, load_source(args.source), load_eda(args.eda)))
    if args.check:
        if not args.output.exists() or args.output.read_text(encoding="utf-8") != rendered:
            raise SystemExit(f"{args.output} is stale; run generate_v3_cases.py")
        print(f"OK: V3 workload is reproducible: {args.output}")
        return 0
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(rendered, encoding="utf-8")
    print(f"Wrote V3 workload to {args.output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
