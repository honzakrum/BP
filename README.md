# Verifying Points-to Analysis in GraalVM Native Image using Judge

## Thesis Information

- **Author**: Jan Křůmal
- **University**: Brno University of Technology
- **Faculty**: Faculty of Information Technology
- **Supervisor**: Ing. David Kozák

## Project Setup and Execution

### Prerequisites
- Java 8+ JDK installed
- GraalVM installed (for native image compilation)
- SDKMAN! recommended for Java version management

### Configuration

1. **Create `jre.conf`** in the JCG root folder with 
your Java installations, example:

```json
[
  {
    "version": 8,
    "path": "/path/to/java/8.0.442-tem"
  },
  {
    "version": 17, 
    "path": "/path/to/java/17.0.8-tem"
  }
]
```

2. **Adjust `NativeImage_config.json`** for GraalVM java and Native 
Image executable paths.

3. **Adjust `run.sh`** for your JCG path:
```bash   
JCG_PATH="/path/to/JCG" 
```
### Execution
Make the run script executable and run it in the JCG folder:

```bash
./run.sh
```

### Output
Result of the testcases will be in `test_results.html` by default, additional information in 
the folder JCG/testcasesOutput.

## Intentionally Excluded Test Cases
The following tests are currently not expected to pass and/or are not supported :

1. **Infrastructure incompatible testcases**
   - Not supported by the test pipeline.

2. **CL4**
   - This test case defines a custom classloader, that loads the class `lib.IntComparator`
     from a byte array. This feature is not supported by GraalVM Native Image.
