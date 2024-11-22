package stability.exceptions;

public class CircuitOpenException extends Exception {
    public CircuitOpenException(String message) {
        super(message);
    }

    public CircuitOpenException() {
        super();
    }
}
