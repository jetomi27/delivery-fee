package deliveryfee.dto;

/**
 * Error returned by the REST API.
 *
 * @param status HTTP status code
 * @param message human readable error description
 */
public record ErrorResponse(int status, String message) {
}