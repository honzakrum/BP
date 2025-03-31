#!/bin/bash

# Tested tool and algorithm
TOOL="NativeImage"      
ALGORITHM="PTA"

# Paths
JCG_PATH="/home/krumi/IdeaProjects/BP/JCG"
INPUT_DIR="input"
OUTPUT_DIR="testcasesOutput/java/native_image_adapter"

# Log files
TEST_COMPILE="test_compile_output.log"
EVALUATION="evaluation_fingerprint.log"

mkdir -p "$OUTPUT_DIR"

# compile tests
echo "Compiling JCG tests..."
touch $TEST_COMPILE
sbt -J-Xmx12G "project jcg_testcases" "runMain TestCaseExtractor --rsrcDir ./jcg_testcases/src/main/resources
--outDir ./input --lang java --debug" &> $TEST_COMPILE
echo "JCG tests compiled, compile output in file ${TEST_COMPILE}."

# run analysis
echo "Running analysis with NativeImageJCGAdapter..."
touch $EVALUATION
sbt -J-Xmx12G "; project jcg_evaluation; runMain FingerprintExtractor -i $JCG_PATH/$INPUT_DIR/java
 -o $JCG_PATH/$OUTPUT_DIR -l java -d --adapter $TOOL --algorithm-prefix $ALGORITHM" #&> $EVALUATION
echo "NativeImageJCGAdapter analysis completed, debug output in file ${EVALUATION}."

# timings
#sbt -J-Xmx12G "; project jcg_evaluation; runMain Evaluation --input $JCG_PATH/$INPUT_DIR --output $JCG_PATH/$OUTPUT_DIR --adapter $TOOL --algorithm-prefix $ALGORITHM"
