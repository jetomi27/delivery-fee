package deliveryfee.exception;

public class WeatherDataFetchException extends RuntimeException{
    public WeatherDataFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}
