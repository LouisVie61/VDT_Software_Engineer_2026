from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any


DEFAULT_SOURCE = Path("evaluation/cases/llm_cases.jsonl")
DEFAULT_OUTPUT = Path("evaluation/cases/v2_cases.jsonl")
DERIVATION_VERSION = "v2-controlled-workflow-1"
ALLOWED_EDITED_INTENT_FIELDS = {
    "intent", "metric", "groupBy", "timeBucket", "topN", "filters", "sort", "timeRange"
}


def canonical_json(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"))


def sha256(value: Any) -> str:
    return hashlib.sha256(canonical_json(value).encode("utf-8")).hexdigest()


def load_source(path: Path) -> list[dict[str, Any]]:
    cases: list[dict[str, Any]] = []
    ids: set[str] = set()
    with path.open("r", encoding="utf-8-sig") as handle:
        for line_number, line in enumerate(handle, start=1):
            if not line.strip():
                continue
            case = json.loads(line)
            for required in ("id", "category", "request", "expected"):
                if required not in case:
                    raise ValueError(f"{path}:{line_number}: missing {required}")
            if case["id"] in ids:
                raise ValueError(f"{path}:{line_number}: duplicate id {case['id']}")
            if not isinstance(case["request"], dict) or not case["request"].get("question"):
                raise ValueError(f"{path}:{line_number}: request.question is required")
            edited = case.get("assisted", {}).get("editedIntent", {})
            unknown = set(edited) - ALLOWED_EDITED_INTENT_FIELDS
            if unknown:
                raise ValueError(f"{path}:{line_number}: unsupported editedIntent fields {sorted(unknown)}")
            ids.add(case["id"])
            cases.append(case)
    if not cases:
        raise ValueError(f"{path}: source is empty")
    return cases


def derive(source: Path, cases: list[dict[str, Any]]) -> list[dict[str, Any]]:
    source_hash = sha256(cases)
    derived: list[dict[str, Any]] = []
    for case in cases:
        item = json.loads(json.dumps(case))
        item["assisted"] = {**item.get("assisted", {}), "scoreFinalResponse": True}
        item["provenance"] = {
            "source": source.name,
            "sourceCaseId": case["id"],
            "sourceCaseSha256": sha256(case),
            "sourceWorkloadSha256": source_hash,
            "derivation": DERIVATION_VERSION,
        }
        derived.append(item)
    return derived


def main() -> int:
    parser = argparse.ArgumentParser(description="Derive the V2 controlled-workflow workload from LLM_cases.jsonl.")
    parser.add_argument("--source", type=Path, default=DEFAULT_SOURCE)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument("--check", action="store_true", help="Fail if output is not exactly reproducible.")
    args = parser.parse_args()

    cases = derive(args.source, load_source(args.source))
    rendered = "".join(canonical_json(case) + "\n" for case in cases)
    if args.check:
        if not args.output.exists() or args.output.read_text(encoding="utf-8") != rendered:
            raise SystemExit(f"{args.output} is stale; run generate_v2_cases.py")
        print(f"OK: {len(cases)} reproducible V2 cases")
        return 0

    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(rendered, encoding="utf-8")
    print(f"Wrote {len(cases)} V2 cases to {args.output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
