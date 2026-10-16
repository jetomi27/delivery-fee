package deliveryfee.exception;

/**
 * Thrown when a request is valid on its own but does not fit the data it refers to
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}