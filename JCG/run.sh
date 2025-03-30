#!/bin/bash

# Tested tool and algorithm
TOOL="NativeImage"      
ALGORITHM="PTA"

# Paths
JCG_PATH="/home/krumi/IdeaProjects/BP/JCG"
INPUT_DIR="input"
OUTPUT_DIR="testcasesOutput/java/native_image_adapter"

# Log files
TEST_COMPILE_STD="test_compile_output.log"
TESTS_COMPILE_ERR="test_compile_output_err.log"
EVALUATION_STD="evaluation_fingerprint.log"
EVALUATION_ERR="evaluation_fingerprint_err.log"

mkdir -p "$OUTPUT_DIR"

# compile tests
echo "Compiling JCG tests..."
touch $TEST_COMPILE_STD
touch $TESTS_COMPILE_ERR
sbt -J-Xmx12G "project jcg_testcases" "runMain TestCaseExtractor --rsrcDir ./jcg_testcases/src/main/resources
--outDir ./input --lang java --debug" > $TEST_COMPILE_STD 2> $TESTS_COMPILE_ERR
echo "JCG tests compiled, compile output in files ${TEST_COMPILE_STD} and ${TESTS_COMPILE_ERR}."

# run analysis
echo "Running analysis with NativeImageJCGAdapter..."
touch $EVALUATION_STD
touch $EVALUATION_ERR
sbt -J-Xmx12G "; project jcg_evaluation; runMain FingerprintExtractor -i $JCG_PATH/$INPUT_DIR/java
 -o $JCG_PATH/$OUTPUT_DIR -l java --adapter $TOOL --algorithm-prefix $ALGORITHM" > $EVALUATION_STD 2> $EVALUATION_ERR
echo "NativeImageJCGAdapter analysis completed, debug output in files ${EVALUATION_STD} and ${EVALUATION_ERR}."

# timings
#sbt -J-Xmx12G "; project jcg_evaluation; runMain Evaluation --input $JCG_PATH/$INPUT_DIR --output $JCG_PATH/$OUTPUT_DIR --adapter $TOOL --algorithm-prefix $ALGORITHM"
