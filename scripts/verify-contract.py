"""Validate the repository's machine-readable API contract without third-party packages."""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
CONTRACT = ROOT / "docs" / "api" / "openapi.json"


def collect_refs(value: Any, refs: list[str]) -> None:
    if isinstance(value, dict):
        reference = value.get("$ref")
        if isinstance(reference, str):
            refs.append(reference)
        for child in value.values():
            collect_refs(child, refs)
    elif isinstance(value, list):
        for child in value:
            collect_refs(child, refs)


def resolve_local(document: dict[str, Any], reference: str) -> Any | None:
    if not reference.startswith("#/"):
        return True
    current: Any = document
    for part in reference[2:].split("/"):
        part = part.replace("~1", "/").replace("~0", "~")
        if not isinstance(current, dict) or part not in current:
            return None
        current = current[part]
    return current


def main() -> int:
    with CONTRACT.open(encoding="utf-8") as handle:
        document = json.load(handle)

    if document.get("openapi") != "3.1.0":
        raise SystemExit("Expected OpenAPI 3.1.0")
    paths = document.get("paths", {})
    if not paths:
        raise SystemExit("OpenAPI contract must define at least one path")

    refs: list[str] = []
    collect_refs(document, refs)
    unresolved = [ref for ref in refs if resolve_local(document, ref) is None]
    if unresolved:
        raise SystemExit(f"Unresolved local references: {unresolved}")

    required_paths = {
        "/api/v1/auth/guest-session",
        "/api/v1/auth/login",
        "/api/v1/tools",
        "/api/v1/tools/{toolId}",
        "/api/v1/tools/{toolId}/execute",
        "/api/v1/admin/tools/{toolId}/policy",
    }
    missing = sorted(required_paths.difference(paths))
    if missing:
        raise SystemExit(f"Missing required paths: {missing}")

    print(f"OPENAPI_CONTRACT_PASS paths={len(paths)} refs={len(refs)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
