package org.reactivewm.thread.is;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;

import org.apache.log4j.Logger;
import org.reactivewm.executor.VolatileISThreadPoolExecutor;

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
	private static final Logger LOG = Logger.getLogger(ListenableFutureTask.class);
	private final ExecutionList executionList = new ExecutionList();
	private Runnable runnable;
	private boolean done;
	private boolean controller;
	private VolatileISThreadPoolExecutor volatilePool;

	public static <V> ListenableFutureTask<V> create(Runnable runnable, boolean controller,
			V result, VolatileISThreadPoolExecutor volatilePool) {
		return new ListenableFutureTask<V>(runnable, controller, result, volatilePool);
	}

	ListenableFutureTask(Runnable runnable, boolean controller, V result, VolatileISThreadPoolExecutor volatilePool) {
		super(runnable, result);
		this.runnable = runnable;
		this.controller = controller;
		this.done = false;
		this.volatilePool = volatilePool;
	}
	
	@Override
	public void addListener(Runnable listener, Executor exec) {
		this.executionList.add(listener, exec);
	}

	public int getPriority() {
		ReactiveServiceThread rst = (ReactiveServiceThread) runnable;
		return rst.getThreadPriority();
	}
	
	@Override
	protected void done() {
		LOG.debug("Listenable done");
		this.executionList.execute();
		if(volatilePool != null) {
			volatilePool.remove(this);
		}
	}
	
	@Override
	public boolean isDone() {
		return done;
	}
	
	@Override
	public V get() throws InterruptedException, ExecutionException {
		return super.get();
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
