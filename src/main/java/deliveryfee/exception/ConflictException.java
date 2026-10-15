package deliveryfee.exception;

/**
 * Thrown when a request conflicts with the current state of the data.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}