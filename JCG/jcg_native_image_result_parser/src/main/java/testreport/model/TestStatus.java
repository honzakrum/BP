package testreport.model;

/**
 * Enumeration of possible evaluation outcomes for a test case.
 *
 * @author Jan Křůmal
 */
public enum TestStatus {
    PASSED, FAILED, IMPRECISE;

    public static TestStatus fromCode(String code) {
        return switch (code) {
            case "S" -> PASSED;
            case "U" -> FAILED;
            case "I" -> IMPRECISE;
            default -> throw new IllegalArgumentException("Unknown status code: " + code);
        };
    }
}