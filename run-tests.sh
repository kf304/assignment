#!/usr/bin/env bash
# Compile and run all JUnit 5 tests without Maven.
set -euo pipefail
cd "$(dirname "$0")"

M2="${HOME}/.m2/repository"
CP="$M2/org/junit/jupiter/junit-jupiter-api/5.10.2/junit-jupiter-api-5.10.2.jar"
CP="$CP:$M2/org/junit/jupiter/junit-jupiter-engine/5.10.2/junit-jupiter-engine-5.10.2.jar"
CP="$CP:$M2/org/junit/jupiter/junit-jupiter-params/5.10.2/junit-jupiter-params-5.10.2.jar"
CP="$CP:$M2/org/junit/platform/junit-platform-commons/1.10.2/junit-platform-commons-1.10.2.jar"
CP="$CP:$M2/org/junit/platform/junit-platform-engine/1.10.2/junit-platform-engine-1.10.2.jar"
CP="$CP:$M2/org/junit/platform/junit-platform-launcher/1.10.2/junit-platform-launcher-1.10.2.jar"
CP="$CP:$M2/org/opentest4j/opentest4j/1.3.0/opentest4j-1.3.0.jar"
CP="$CP:$M2/org/apiguardian/apiguardian-api/1.1.2/apiguardian-api-1.1.2.jar"

OUT=build
rm -rf "$OUT"
mkdir -p "$OUT/classes" "$OUT/test-classes"

find src/main/java -name '*.java' > "$OUT/main.txt"
javac -d "$OUT/classes" @"$OUT/main.txt"

find src/test/java -name '*.java' > "$OUT/test.txt"
javac -cp "$OUT/classes:$CP" -d "$OUT/test-classes" @"$OUT/test.txt"

javac -cp "$CP" -d "$OUT/test-classes" tools/TestRunner.java

java -cp "$OUT/classes:$OUT/test-classes:$CP" TestRunner "${1:-com.example.support}"
