# Verifying Points-to Analysis in GraalVM Native Image using Judge

## Thesis Information

- **Author**: Jan Křůmal
- **University**: Brno University of Technology
- **Faculty**: Faculty of Information Technology
- **Supervisor**: Ing. David Kozák


## Intentionally Excluded Test Cases
The following tests are currently not expected to pass and/or are not supported :

1. **Infrastructure incompatible testcases**
   - Not supported by the test pipeline.

2. **CL4**
    - This test case defines a custom classloader, that loads the class `lib.IntComparator` 
   from a byte array. This feature is not supported by GraalVM Native Image.

