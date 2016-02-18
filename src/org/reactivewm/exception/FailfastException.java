package org.reactivewm.exception;

/**
 * Thread exception
 * @author Teiva Harsanyi
 *
 */
public class FailfastException extends Exception {

	private static final long serialVersionUID = 9255554557L;

	public FailfastException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public FailfastException(String message) {
		super(message);
	}
}
