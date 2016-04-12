package org.reactivewm.executor;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.reactivewm.thread.Priority;
import org.reactivewm.thread.is.ListenableFutureTask;
import org.reactivewm.thread.is.ReactiveServiceThread;

public final class VolatileISThreadPoolExecutor extends ISThreadPoolExecutor {
	private static final Logger LOG = Logger.getLogger(VolatileISThreadPoolExecutor.class);
	private AtomicInteger currentPriority;
	private String currentRef;
	private Date limit;
	private Map<String, Integer> priorities;
	private Map<Future<?>, String> executionList;
	private boolean atomic;

	public VolatileISThreadPoolExecutor(String poolName, Date limit, int corePoolSize, int maximumPoolSize,
			long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory,
			ThreadExecutable executable, boolean atomic) {
		super(poolName, corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, executable, true);
		LOG.debug("Volatile creation: " + poolName);
		if (limit == null) {
			throw new NullPointerException("Limit date is null");
		}
		this.limit = limit;
		this.currentRef = null;
		this.currentPriority = new AtomicInteger(Priority.MID_PRIORITY);
		this.priorities = new ConcurrentHashMap<String, Integer>();
		this.executionList = new ConcurrentHashMap<Future<?>, String>();
		this.atomic = atomic;
	}

	public void updateLimit(Date limit) {
		this.limit = limit;
	}
	
	@Override
	public Future<?> submit(Runnable task, boolean controller) {
		if (task == null) {
			throw new NullPointerException();
		}
		LOG.debug("Volatile submit: executionList="+executionList.size() + ", "  + task.getClass());
		RunnableFuture<Object> ftask = null;
		if(atomic) {
			ReactiveServiceThread rst = (ReactiveServiceThread)task;
			int priority = rst.getThreadPriority();
			
			synchronized(this) {
				if(priority > currentPriority.get()) {
					LOG.debug("Priority sup");
					clear();
					ftask = newTaskFor(task, controller, null);
					executionList.put(ftask, "");
				} else if(priority < currentPriority.get()) {
					LOG.debug("Priority inf");
					return null;
				} else {
					LOG.debug("Priority equals");
					ftask = newTaskFor(task, controller, null);
					executionList.put(ftask, "");
				}
			}
			execute(ftask);
			
			return ftask;
		} else {
			ftask = newTaskFor(task, controller, null);
			execute(ftask);
			return ftask;
		}
	}
	
	@Override
	protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, boolean controller, T value) {
		return ListenableFutureTask.create(runnable, controller,
				value, this);
	}
	
	public void complete(Future<?> future) {
		synchronized(this) {
			this.executionList.remove(future);
		}
	}
	
	private void clear() {
		LOG.debug("Clearing pool");
		for (Map.Entry<Future<?>, String> entry : executionList.entrySet()) {
		    Future<?> future = entry.getKey();
		    future.cancel(false);
		}
	}
	
	public int getPriority(String ref) {
		if (ref == null) {
			throw new NullPointerException("Thread priority reference is null");
		}

		if (priorities.containsKey(ref)) {
			int priority = priorities.get(ref);
			return priority;
		}

		if (currentRef == null) {
			int priority = currentPriority.get();
			LOG.debug("Volatile priority first: priority=" + priority + ", ref=" + ref);
			if (!priorities.containsKey(ref)) {
				priorities.put(ref, priority);
			}

			currentRef = ref;

			return priority;
		} else if (ref.compareTo(currentRef) > 0) {
			int priority = currentPriority.incrementAndGet();
			LOG.debug("Volatile priority sup: priority=" + priority + ", ref=" + ref);
			if (!priorities.containsKey(ref)) {
				priorities.put(ref, priority);
			}

			currentRef = ref;

			if (priority >= Priority.MAX_PRIORITY) {
				return Priority.MAX_PRIORITY;
			} else {
				return priority;
			}
		} else if (ref.compareTo(currentRef) < 0) {
			int priority = currentPriority.get();
			priority = (priority - 1) <= Priority.MIN_PRIORITY ? Priority.MIN_PRIORITY : priority - 1;
			priority = priority >= Priority.MAX_PRIORITY ? Priority.MAX_PRIORITY : priority;
			
			LOG.debug("Volatile priority inf: priority=" + priority + ", ref=" + ref);
			if (!priorities.containsKey(ref)) {
				priorities.put(ref, priority);
			}

			return priority;
		} else {
			int priority = currentPriority.get();
			LOG.debug("Volatile priority equals: priority=" + priority + ", ref=" + ref);
			if (!priorities.containsKey(ref)) {
				priorities.put(ref, priority);
			}

			return priority;
		}
	}

	public boolean isLimitReached() {
		Date cur = new Date();

		return cur.compareTo(limit) > 0;
	}
}
