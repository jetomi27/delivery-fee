package deliveryfee.exception;

/**
 * Thrown when weather conditions forbid using the selected vehicle type
 */
public class VehicleUsageForbiddenException extends RuntimeException {

    public VehicleUsageForbiddenException() {
        super("Usage of selected vehicle type is forbidden");
    }
}