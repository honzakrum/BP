#!/bin/bash

# Positional parameter: JCG repo path
JCG_PATH="$1"

# Safety check
if [ -z "$JCG_PATH" ]; then
  echo "[error] Missing argument: path to JCG repository."
  exit 1
fi

cd "$JCG_PATH" || { echo "[error] Could not cd into $JCG_PATH"; exit 1; }

echo "[info] Running sbt clean..."
sbt clean

# List of folders to delete if they exist
FOLDERS=("input" "subset_input" "testcasesOutput" "config" "CallGraphs")

for folder in "${FOLDERS[@]}"; do
  if [ -d "$folder" ]; then
    echo "[info] Deleting folder: $folder"
    rm -rf "$folder"
  fi
done

# Check and delete results as well
if [ -f "test_results.html" ]; then
  echo "[info] Deleting file: test_results.html"
  rm -f "test_results.html"
fi

echo "[done] Cleanup complete."