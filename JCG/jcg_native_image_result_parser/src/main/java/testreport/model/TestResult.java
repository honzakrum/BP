package testreport.model;

import java.nio.file.Path;

/**
 * Represents a single test case result within the Native Image evaluation framework.
 *
 * Stores:
 * - Test name and evaluation status.
 * - Enriched metadata including readable description, code example, logs, and matcher output.
 * - Paths to generated artifacts such as JSON call graph, CSVs, and configuration files.
 *
 * @author Jan Křůmal
 */
public class TestResult {
    private String name;
    private TestStatus status;
    private String description = "No description available";
    private String testCase = "No test case details available";
    private String log = "No log available";
    private String matcherOutput = "No matcher output available";

    private Path jsonPath;
    private Path csvMethods;
    private Path csvInvokes;
    private Path csvTargets;
    private Path configFile;

    public TestResult(String name, TestStatus status) {
        this.name = name;
        this.status = status;
    }

    public String getName() { return name; }
    public TestStatus getStatus() { return status; }
    public String getDescription() { return description; }
    public String getTestCase() { return testCase; }
    public String getLog() { return log; }
    public String getMatcherOutput() { return matcherOutput; }

    public Path getJsonPath() { return jsonPath; }
    public Path getCsvMethods() { return csvMethods; }
    public Path getCsvInvokes() { return csvInvokes; }
    public Path getCsvTargets() { return csvTargets; }
    public Path getConfigFile() { return configFile; }

    public void setDescription(String description) { this.description = description; }
    public void setTestCase(String testCase) { this.testCase = testCase; }
    public void setLog(String log) { this.log = log; }
    public void setMatcherOutput(String matcherOutput) { this.matcherOutput = matcherOutput; }

    public void setJsonPath(Path jsonPath) { this.jsonPath = jsonPath; }
    public void setCsvMethods(Path csvMethods) { this.csvMethods = csvMethods; }
    public void setCsvInvokes(Path csvInvokes) { this.csvInvokes = csvInvokes; }
    public void setCsvTargets(Path csvTargets) { this.csvTargets = csvTargets; }
    public void setConfigFile(Path configFile) { this.configFile = configFile; }
}
