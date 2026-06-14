#!/usr/bin/env bash
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"

if [[ $# -eq 0 ]]; then
  VERSIONS=("1.6.1" "master")
else
  VERSIONS=("$@")
fi

# Pre-flight: every requested release (everything except "master", which is
# built locally) must resolve from Maven Central before we run anything. A
# version that does not resolve used to be silently skipped, so it quietly
# vanished from the docs — e.g. a typo'd "1.9.1", or a release dispatched on
# its release day before it had finished syncing to Central. Collect every
# unresolvable version and fail loudly up front rather than producing a green
# run with missing results.
MISSING=()
for V in "${VERSIONS[@]}"; do
  [[ "$V" == "master" ]] && continue
  if ! mvn dependency:get \
       -Dartifact="com.ancientprogramming.fixedformat4j:fixedformat4j:${V}" \
       -q >/dev/null 2>&1; then
    MISSING+=("$V")
  fi
done
if [[ ${#MISSING[@]} -gt 0 ]]; then
  echo "::error::Unresolvable fixedformat4j version(s): ${MISSING[*]}" >&2
  echo "Each non-master version must exist and have finished syncing to Maven Central. Fix the version list and re-run." >&2
  exit 1
fi

OUT_DIR="$(pwd)/docs/assets/benchmarks"
mkdir -p "$OUT_DIR"

INDEX="$OUT_DIR/index.json"
if [[ ! -f "$INDEX" ]]; then
  echo '{"versions":[]}' > "$INDEX"
fi

for V in "${VERSIONS[@]}"; do
  if [[ "$V" == "master" ]]; then
    mvn -pl fixedformat4j -am install -DskipTests -q
    TARGET_VERSION="$(mvn -f fixedformat4j/pom.xml help:evaluate -Dexpression=project.version -q -DforceStdout)"
    LABEL="master"
  else
    TARGET_VERSION="$V"
    LABEL="$V"
  fi

  SHA="$(git rev-parse --short HEAD)"
  mvn -f benchmarks/pom.xml clean package -Dbenchmark.target.version="$TARGET_VERSION" -q
  java -jar benchmarks/target/benchmarks.jar \
       -rf json \
       -rff "$OUT_DIR/${LABEL}.json" \
       -foe true

  cat > "$OUT_DIR/${LABEL}.meta.json" <<EOF
{ "label": "${LABEL}", "resolvedVersion": "${TARGET_VERSION}", "gitSha": "${SHA}", "javaVersion": "$(java -version 2>&1 | head -1)", "timestamp": "$(date -u +%FT%TZ)" }
EOF

  if command -v jq &>/dev/null; then
    jq --arg v "$LABEL" 'if (.versions | index($v)) then . else .versions += [$v] end' "$INDEX" > "${INDEX}.tmp" && mv "${INDEX}.tmp" "$INDEX"
  else
    python3 - "$INDEX" "$LABEL" <<'PYEOF'
import json, sys
path, label = sys.argv[1], sys.argv[2]
idx = json.load(open(path))
if label not in idx['versions']:
    idx['versions'].append(label)
with open(path, 'w') as f:
    json.dump(idx, f)
PYEOF
  fi
done
