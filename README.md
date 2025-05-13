# Verifying Points-to Analysis in GraalVM Native Image using Judge

## Thesis Information

- **Author**: Jan Křůmal
- **University**: Brno University of Technology
- **Faculty**: Faculty of Information Technology
- **Supervisor**: Ing. David Kozák

## Project Overview

This project extends the [OPAL JCG project](https://github.com/opalj/JCG) to support call graph extraction and points-to analysis for programs compiled with **GraalVM Native Image**.

### Contributions

The key components developed as part of this work:

- **`jcg_native_image_testadapter`**  
  A specialized adapter that integrates GraalVM Native Image analysis results into the JCG (Java Call Graph) analysis pipeline. This component is responsible for orchestrating the execution of Native Image analysis,   including the generation of configuration files and execution parameters. It ensures compatibility with the JCG framework by producing call graphs in the required format.

- **`jcg_native_image_result_parser`**  
  This parser transforms raw profiling results, logs, and call graph data into a structured and navigable HTML report, providing clear visualization and easier analysis of the JCG produced results.

## Setup and Execution

### Prerequisites

- Java 8+ JDK
- [GraalVM](https://www.graalvm.org/) (must include `native-image`)
- [SDKMAN!](https://sdkman.io) for managing multiple Java versions (optional but recommended)
- sbt `1.10.7`
- Maven (or Maven Wrapper)
- `mx` build tool (used to drive the analysis pipeline)

### Environment Configuration

1. **Set `GRAAL_HOME` environment variable**

```bash
export GRAAL_HOME=/path/to/your/graalvm
```

Make sure it includes:

```
$GRAAL_HOME/bin/java

$GRAAL_HOME/bin/native-image
```
2. **Create `jre.conf` in the JCG repo**

This file lists the JDKs used by the JCG test framework. Example contents:

```json
[
  {
    "version": 8,
    "path": "/path/to/java/8"
  },
  ...
]
```
This file is used by JCG to configure JDK versions for analysis.


### Running the Pipeline via `mx judge`
All commands are invoked using the `mx` tool via a custom `judge` command.

> ⚠️ **Important:** Make sure you're inside the `substratevm` suite of GraalVM, or that it’s registered with `mx`.

#### Enabling the `judge` Command

To enable the `judge` command, apply the `mx-judge-command.diff` patch.  
This patch should be applied to the `/substratevm/mx.substratevm/mx_substratevm.py` file.

### Compile All Testcases

Compiles all Java test cases defined in JCG:

```bash
mx judge compile --jcg-path /path/to/JCG --mem-limit 16G
```

- Output is stored in:  
  `/path/to/JCG/testcasesOutput/java/native_image_adapter/test_compile_output.log`



### Run Evaluation

#### Run all compiled tests

```bash
mx judge run --jcg-path /path/to/JCG
```

#### Run a specific category of tests

```bash
mx judge run --jcg-path /path/to/JCG --category CFNE
```

Examples of categories: `CSR`, `DFNE`, `Ser`, `JVMC`, etc.

#### Run a specific single test

```bash
mx judge run --jcg-path /path/to/JCG --test CFNE3
```

> Test files must exist as `CFNE3.jar` and `CFNE3.conf` in the input folder.

#### Control memory limit

All commands except the `clean` accept the `--mem-limit` option, default is 12G:

```bash
mx judge compile --jcg-path /path/to/JCG --mem-limit 24G
mx judge run --jcg-path /path/to/JCG --mem-limit 24G
```

#### Clean Project Artifacts

You can remove generated files and reset the repository to a clean state using:

```bash
mx judge clean --jcg-path /path/to/JCG
```

This performs the following actions:

- Runs `sbt clean` to delete all `target/` directories
- Deletes the following folders if they exist:
  - `input/`
  - `subset_input/`
  - `testcasesOutput/`
  - `config/`
  - `CallGraphs/`

Use this command when you want to start with a clean slate, especially before recompiling test cases or rerunning evaluations.


#### Clone JCG Repo

This feature allows you to automatically clone this repository into a target directory:

```bash
mx judge clone --into /target/folder
```

## Output

- **Evaluation log**:  
  `testcasesOutput/java/native_image_adapter/evaluation.log`

- **Parsed results**:  
  Path to the `HTML report` is printed to console after evaluation.


## Intentionally Excluded Test Cases

Some test cases are known to be incompatible with GraalVM Native Image or the evaluation infrastructure:

### Infrastructure-Incompatible Test Cases

Require special build steps (e.g., Scala compilation, bytecode engineering) incompatible with the standard pipeline.

### LIB Test Cases
Standalone libraries without a `main` method; cannot produce native binaries without an entry point.


## Acknowledgements

This project is built on top of the [OPAL JCG framework](https://github.com/opalj/JCG).  
All integration work for GraalVM Native Image was done as part of the author’s thesis.

