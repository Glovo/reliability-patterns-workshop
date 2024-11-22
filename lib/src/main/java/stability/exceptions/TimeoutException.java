package stability.exceptions;

// Custom Exceptions
public class TimeoutException extends Exception {
    public TimeoutException(String message) {
        super(message);
    }

    public TimeoutException() {
        super();
    }
}
