#!/usr/bin/env bash
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"

if [[ $# -eq 0 ]]; then
  VERSIONS=("1.6.1" "master")
else
  VERSIONS=("$@")
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

  if [[ "$V" != "master" ]]; then
    if ! mvn dependency:get \
         -Dartifact="com.ancientprogramming.fixedformat4j:fixedformat4j:${TARGET_VERSION}" \
         -q >/dev/null 2>&1; then
      echo "Skipping ${LABEL}: fixedformat4j:${TARGET_VERSION} not found in Maven Central." >&2
      continue
    fi
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
