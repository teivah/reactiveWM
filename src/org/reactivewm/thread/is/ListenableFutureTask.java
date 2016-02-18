package org.reactivewm.thread.is;

import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;

import com.google.common.util.concurrent.ExecutionList;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * FutureTask implementing a Guava ListenableFuture
 * @author Teiva Harsanyi
 *
 * @param <V>
 */
public class ListenableFutureTask<V> extends FutureTask<V> implements
		ListenableFuture<V> {
	private final ExecutionList executionList = new ExecutionList();
	private Runnable runnable;
	private boolean done;
	private boolean controller;

	public static <V> ListenableFutureTask<V> create(Runnable runnable, boolean controller,
			V result) {
		return new ListenableFutureTask<V>(runnable, controller, result);
	}

	ListenableFutureTask(Runnable runnable, boolean controller, V result) {
		super(runnable, result);
		this.runnable = runnable;
		this.controller = controller;
		this.done = false;

	}
	
	@Override
	public void addListener(Runnable listener, Executor exec) {
		this.executionList.add(listener, exec);
	}

	@Override
	protected void done() {
		this.executionList.execute();
	}
	
	@Override
	public boolean isDone() {
		return done;
	}
	
	@Override
	public void set(V arg0) {
		super.set(arg0);
		done = true;
	}
	
	@Override
	public void setException(Throwable arg0) {
		super.setException(arg0);
	}
	
	public void setRunnable(Runnable runnable) {
		this.runnable = runnable;
	}
	
	public void setDone(boolean done) {
		this.done = done;
	}
	
	public Runnable getRunnable() {
		return runnable;
	}
	
	public boolean isController() {
		return controller;
	}
}
