package org.reactivewm.thread;

import java.util.concurrent.ThreadFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Tread factory
 * This class might be overridden in case of a client wants to implement its own management of uncaught exceptions
 * @author Teiva Harsanyi
 *
 */
public class ISThreadFactory implements ThreadFactory {
	private static final Logger LOG = Logger.getLogger(ISThreadFactory.class);
	
	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(r);
		t.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread thread, Throwable throwable) {
				LOG.log(Level.ERROR, "Uncaught thread exception: " + throwable.getMessage());
			}
		});
		return t;
	}
}
