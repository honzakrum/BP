#!/bin/bash

# JAR test files location
FOLDER="./input/java"

# Loop through each .jar file and print its name
# TODO run NI to create call graphs
for jar in "$FOLDER"/*.jar; do
    # Check for matches
    [ -e "$jar" ] || continue
    # print
    echo "$(basename "$jar")"
    # create call graph files
done
