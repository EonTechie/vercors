from pathlib import Path
import subprocess
import os
import re

dirp = Path(__file__).resolve().parent
vercors = Path(r"C:\vercors-2.4.0-win\vercors.bat")
llvm = r"C:\Program Files\LLVM\bin"
java_bin = r"C:\Program Files\Microsoft\jdk-25.0.2.10-hotspot\bin"

env = os.environ.copy()
env["Path"] = java_bin + os.pathsep + llvm + os.pathsep + env.get("Path", "")
env["JAVA_HOME"] = r"C:\Program Files\Microsoft\jdk-25.0.2.10-hotspot"

files = sorted(dirp.glob("*_transformed_added_annotations.c"))
print(f"verifying {len(files)} files")

summary = []
for f in files:
    r = subprocess.run(
        ["cmd", "/c", str(vercors), str(f.resolve())],
        capture_output=True,
        text=True,
        env=env,
        cwd=str(dirp),
    )
    out = (r.stdout or "") + (r.stderr or "")
    ok = "Verification completed successfully" in out
    status = "PASS" if ok else "FAIL"
    summary.append(f"{status} {f.name}")
    print(f"{status} {f.name}")
    if not ok:
        # keep last meaningful lines
        lines = [ln for ln in out.splitlines() if ln.strip()]
        for ln in lines[-25:]:
            print("  ", ln)

(dirp / "_verify_summary.txt").write_text("\n".join(summary) + "\n", encoding="utf-8")
npass = sum(1 for s in summary if s.startswith("PASS"))
nfail = sum(1 for s in summary if s.startswith("FAIL"))
print(f"TOTAL pass={npass} fail={nfail}")
