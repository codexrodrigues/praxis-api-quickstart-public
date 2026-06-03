#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "$ROOT_DIR"

mkdir -p target

./mvnw -q -DincludeScope=runtime -Dmdep.outputFile=target/runtime-classpath.txt dependency:build-classpath

javac -cp "$(cat target/runtime-classpath.txt)" -d target/scripts scripts/DomainKnowledgeProjectionValidation.java

java -cp "target/scripts:$(cat target/runtime-classpath.txt)" DomainKnowledgeProjectionValidation "$@"
