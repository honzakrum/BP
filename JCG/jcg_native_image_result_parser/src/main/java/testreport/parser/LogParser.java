package testreport.parser;

import testreport.model.TestResult;
import testreport.util.HtmlUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class LogParser {

    public void enrichWithLogs(Path logFile, List<TestResult> results) throws IOException {
        Map<String, TestResult> resultMap = new HashMap<>();
        for (TestResult r : results) {
            resultMap.put(r.getName(), r);
        }

        Map<String, StringBuilder> logMap = new HashMap<>();
        Map<String, StringBuilder> matcherMap = new HashMap<>();

        String currentTest = null;
        StringBuilder currentLog = new StringBuilder();
        StringBuilder currentMatcher = new StringBuilder();
        boolean inMatcherBlock = false;

        for (String line : Files.readAllLines(logFile)) {
            if (line.contains("performing test case:")) {
                if (currentTest != null) {
                    logMap.put(currentTest, new StringBuilder(currentLog));
                    matcherMap.put(currentTest, new StringBuilder(currentMatcher));
                }

                currentTest = line.substring(line.indexOf(":") + 1).trim();
                currentLog = new StringBuilder();
                currentMatcher = new StringBuilder();
                inMatcherBlock = false;

            } else if (currentTest != null) {
                currentLog.append(line).append("\n");

                if (line.contains("[info][CG matcher]")) {
                    inMatcherBlock = true;
                }

                if (inMatcherBlock) {
                    currentMatcher.append(line).append("\n");
                    if (line.trim().isEmpty()) {
                        inMatcherBlock = false;
                    }
                }
            }
        }

        // Last test case
        if (currentTest != null) {
            logMap.put(currentTest, currentLog);
            matcherMap.put(currentTest, currentMatcher);
        }

        for (Map.Entry<String, TestResult> entry : resultMap.entrySet()) {
            String testName = entry.getKey();
            TestResult result = entry.getValue();

            if (logMap.containsKey(testName)) {
                result.setLog(HtmlUtils.escape(logMap.get(testName).toString()));
            }
            if (matcherMap.containsKey(testName)) {
                result.setMatcherOutput(HtmlUtils.escape(matcherMap.get(testName).toString()));
            }
        }
    }
}
