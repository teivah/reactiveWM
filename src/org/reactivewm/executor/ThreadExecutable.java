package org.reactivewm.executor;

public interface ThreadExecutable {
	public void execute(Runnable task, String poolName);
}
