package project.project.Exception;

public class CartAvailabilityException extends IllegalArgumentException {
    private final String code;

    public CartAvailabilityException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() { return code; }
}
