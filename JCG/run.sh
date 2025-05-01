#!/bin/bash

# Positional parameters
JCG_PATH="$1"
INPUT_DIR="$2"         # Expected: input/java or subset_input/java
MEM_LIMIT="${3:-12G}"  # Default to 12G if not provided

# Constants
TOOL="NativeImage"
ALGORITHM="PTA"
OUTPUT_DIR="testcasesOutput/java/native_image_adapter"
PARSER_DIR="jcg_native_image_result_parser"
EVALUATION_LOG="$OUTPUT_DIR/evaluation.log"
PROFILE_FILE="$OUTPUT_DIR/NativeImage-PTA.profile"
RESOURCE_DIR="jcg_testcases/src/main/resources"

# Move into JCG repo root
cd "$JCG_PATH" || { echo "[error] Could not cd into $JCG_PATH"; exit 1; }
mkdir -p "$OUTPUT_DIR"

# --- Run evaluation ---
echo "[info] Running NativeImageJCGAdapter analysis..."
touch "$EVALUATION_LOG"
sbt -J-Xmx"$MEM_LIMIT" "; project jcg_evaluation; runMain FingerprintExtractor -i $JCG_PATH/$INPUT_DIR \
  -o $JCG_PATH/$OUTPUT_DIR -l java -d --adapter $TOOL --algorithm-prefix $ALGORITHM" &> "$EVALUATION_LOG"

if [ $? -ne 0 ]; then
    echo "[error] Evaluation failed. Check $EVALUATION_LOG"
    exit 1
fi

echo "[done] Evaluation completed. Log written to: $EVALUATION_LOG"

# --- Parse results ---
echo "[info] Building and running the result parser..."
cd "$PARSER_DIR" || { echo "[error] Parser directory not found at $PARSER_DIR"; exit 1; }

# Use Maven wrapper if present
if [ -f "./mvnw" ]; then
    MAVEN="./mvnw"
else
    MAVEN="mvn"
fi

$MAVEN clean package || { echo "[error] Maven build failed"; exit 1; }

cd ..
java -jar "$PARSER_DIR/target/jcg_native_image_result_parser-1.0-SNAPSHOT.jar" \
    "$PROFILE_FILE" "$EVALUATION_LOG" "$RESOURCE_DIR/java"

echo "[done] Result parsing completed."