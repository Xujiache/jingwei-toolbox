"""Validate versioned tool contract bundles without third-party packages."""

from __future__ import annotations

import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "docs" / "tools"
TOOL_ID = re.compile(r"^[a-z0-9]+(?:-[a-z0-9]+)*$")
SEMVER = re.compile(r"^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)$")


def read_json(path: Path) -> object:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        raise SystemExit(f"Invalid JSON: {path}: {exc}") from exc


def main() -> int:
    if not TOOLS.is_dir():
        raise SystemExit("docs/tools directory is missing")

    bundles = [path for path in TOOLS.iterdir() if path.is_dir() and path.name != "TEMPLATE"]
    for bundle in bundles:
        manifest = read_json(bundle / "manifest.json")
        if not isinstance(manifest, dict):
            raise SystemExit(f"manifest must be an object: {bundle}")
        tool_id = manifest.get("toolId")
        version = manifest.get("version")
        if not isinstance(tool_id, str) or not TOOL_ID.fullmatch(tool_id):
            raise SystemExit(f"invalid toolId in {bundle / 'manifest.json'}")
        if bundle.name != tool_id:
            raise SystemExit(f"directory name must equal toolId: {bundle}")
        if not isinstance(version, str) or not SEMVER.fullmatch(version):
            raise SystemExit(f"invalid SemVer version in {bundle / 'manifest.json'}")
        for field in ("displayName", "description", "executorKey", "executionMode", "minClientVersion"):
            if not isinstance(manifest.get(field), str) or not manifest[field].strip():
                raise SystemExit(f"manifest field {field} is required: {bundle / 'manifest.json'}")
        if manifest.get("executionMode") != "SYNC_JSON":
            raise SystemExit(f"only SYNC_JSON is supported in this phase: {bundle / 'manifest.json'}")
        if not isinstance(manifest.get("timeoutMs"), int) or not 100 <= manifest["timeoutMs"] <= 300000:
            raise SystemExit(f"timeoutMs must be 100..300000: {bundle / 'manifest.json'}")
        access = manifest.get("access")
        if not isinstance(access, dict) or not all(key in access for key in ("guest", "user", "member", "adRewarded")):
            raise SystemExit(f"manifest access must define guest/user/member/adRewarded: {bundle / 'manifest.json'}")
        for filename in ("input.schema.json", "output.schema.json", "errors.json", "test-cases.json"):
            value = read_json(bundle / filename)
            if filename.endswith("schema.json") and not isinstance(value, dict):
                raise SystemExit(f"schema must be an object: {bundle / filename}")

    print(f"TOOL_CONTRACT_PASS bundles={len(bundles)} template={'present' if (TOOLS / 'TEMPLATE').is_dir() else 'missing'}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
