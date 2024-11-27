package stability.exceptions;

// Custom Exceptions
public class MaxRetriesException extends Exception {
    public MaxRetriesException(String message) {
        super(message);
    }

    public MaxRetriesException() {
        super();
    }
}