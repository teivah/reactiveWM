package org.reactivewm.exception;

/**
 * Thread exception
 * @author Teiva Harsanyi
 *
 */
public class ThreadException extends Exception {

	private static final long serialVersionUID = 8414554156L;

	public ThreadException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public ThreadException(String message) {
		super(message);
	}
	
	public ThreadException(Throwable cause) {
		super(cause);
	}
}
