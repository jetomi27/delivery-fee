package deliveryfee.exception;

/**
 * Thrown when requested data does not exist
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}