#!/bin/bash

# Positional parameters
JCG_PATH="$1"
MEM_LIMIT="${2:-12G}"

# Constants
OUTPUT_DIR="testcasesOutput/java/native_image_adapter"
RESOURCE_DIR="jcg_testcases/src/main/resources"
TEST_COMPILE_LOG="$OUTPUT_DIR/test_compile_output.log"

cd "$JCG_PATH" || exit 1
mkdir -p "$OUTPUT_DIR"

echo "[info] Compiling JCG tests..."
touch "$TEST_COMPILE_LOG"

sbt -J-Xmx"$MEM_LIMIT" "project jcg_testcases" "runMain TestCaseExtractor \
  --rsrcDir $RESOURCE_DIR \
  --outDir input --lang java --debug" &> "$TEST_COMPILE_LOG"

if [ $? -ne 0 ]; then
    echo "[error] Compilation failed. Check $TEST_COMPILE_LOG for details."
    exit 1
fi

echo "[done] Compilation completed. Output log: $TEST_COMPILE_LOG"