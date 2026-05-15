from __future__ import annotations

import argparse
import os
import shutil
import subprocess
import sys
from pathlib import Path


def _find_powershell() -> str | None:
    for candidate in ("powershell.exe", "pwsh.exe", "pwsh"):
        resolved = shutil.which(candidate)
        if resolved:
            return resolved
    return None


def _run_command(args: list[str], *, cwd: Path | None = None) -> None:
    pretty = " ".join(args)
    print(f"==> {pretty}")
    subprocess.run(args, cwd=None if cwd is None else str(cwd), check=True)


def _resolve_output_dir(repo_root: Path, output_dir: str) -> Path:
    path = Path(output_dir)
    if not path.is_absolute():
        path = repo_root / path
    return path.resolve()


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(
        description="One-click Windows installer build for this repo (wrapper around todo/build-installer.ps1)."
    )
    parser.add_argument(
        "--output-dir",
        default="dist",
        help="Output folder for the installer (default: dist, relative to repo root).",
    )
    args = parser.parse_args(argv)

    if os.name != "nt":
        print("This packaging pipeline currently targets Windows (Inno Setup + jpackage).", file=sys.stderr)
        return 2

    repo_root = Path(__file__).resolve().parent
    ps_script = repo_root / "todo" / "build-installer.ps1"
    if not ps_script.is_file():
        print(f"Missing build script: {ps_script}", file=sys.stderr)
        return 2

    powershell = _find_powershell()
    if not powershell:
        print("Could not find PowerShell (powershell.exe / pwsh).", file=sys.stderr)
        return 2

    if not shutil.which("mvn"):
        print("Could not find Maven on PATH (`mvn`). Please install Maven or add it to PATH.", file=sys.stderr)
        return 2

    resolved_output_dir = _resolve_output_dir(repo_root, args.output_dir)
    resolved_output_dir.mkdir(parents=True, exist_ok=True)

    ps_args = [
        powershell,
        "-NoProfile",
        "-ExecutionPolicy",
        "Bypass",
        "-File",
        str(ps_script),
        "-OutputDir",
        str(resolved_output_dir),
    ]
    _run_command(ps_args, cwd=repo_root)
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))

