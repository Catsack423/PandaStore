package project.project.Exception;

public class InvalidApplicationDataException extends RuntimeException {
    public InvalidApplicationDataException(String message) {
        super(message);
    }

    public InvalidApplicationDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
