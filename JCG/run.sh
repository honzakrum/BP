#!/bin/bash

# project name and tools
PROJECT="test_project" 
TOOL="NativeImage"      
ALGORITHM="RTA"         
RUNS=1                

# Paths
JCG_PATH="/home/krumi/IdeaProjects/BP/JCG"
INPUT_DIR="input/java"
OUTPUT_DIR="testcasesOutput/java/native_image_adapter"

mkdir -p "$OUTPUT_DIR"

# compiling
#echo "Building project with NativeImageJCGAdapter..."
#sbt -java-home /opt/jdk8u342-b07/jre -J-Xmx400G "; project jcg_evaluation; compile"
#sbt "; project jcg_evaluation; compile"

# run
echo "Running analysis with NativeImageJCGAdapter..."

sbt "; project jcg_evaluation; runMain Evaluation --input $JCG_PATH/$INPUT_DIR --output
$JCG_PATH/$OUTPUT_DIR --adapter $TOOL --algorithm-prefix $ALGORITHM"


echo "Analysis completed. Results are in $OUTPUT_DIR."

# debug opt -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005