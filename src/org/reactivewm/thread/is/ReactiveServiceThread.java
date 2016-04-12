package org.reactivewm.thread.is;

import org.apache.log4j.Logger;

import com.wm.app.b2b.server.ServiceThread;
import com.wm.app.b2b.server.Session;
import com.wm.app.b2b.server.ThreadManager;
import com.wm.data.IData;
import com.wm.lang.ns.NSName;

/**
 * Extension of a webMethods ServiceThread to add cancel and priority definition
 * capabilities
 * 
 * @author Teiva Harsanyi
 * 
 */
public class ReactiveServiceThread extends ServiceThread {
	private static final Logger LOG = Logger.getLogger(ReactiveServiceThread.class);
	private final Object lock = new Object();
	private boolean cancel;
	private boolean run;
	private boolean interruptable;
	private int threadPriority;
	private String id;

	public ReactiveServiceThread(NSName service, Session session, IData input,
			int threadPriority, boolean interruptable) {
		super(service, session, input);
		this.cancel = false;
		this.run = false;
		this.id = null;
		this.threadPriority = threadPriority;
		this.interruptable = interruptable;
	}

	public void cancel() {
		synchronized (lock) {
			if (interruptable) {
				if (run) {
					if (id != null) {
						ThreadManager.getThreadManagerImpl().interrupt(id);
					} else {
						try {
							Thread.sleep(200);
						} catch (InterruptedException e) {
						}
						if (id != null) {
							ThreadManager.getThreadManagerImpl().interrupt(id);
						}
					}
				} else {
					this.cancel = true;
				}
			} else {
				this.cancel = true;
			}
		}
	}

	public int getThreadPriority() {
		LOG.debug("Thread priority=" + threadPriority);
		return threadPriority;
	}

	public boolean isCancelled() {
		synchronized (lock) {
			return this.cancel;
		}
	}

	@Override
	public void run() {
		synchronized (lock) {
			this.run = true;
		}
		super.run();
		synchronized (lock) {
			this.run = false;
		}
	}

	public void setId(String id) {
		this.id = id;
	}
}
