package org.reactivewm.facade;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.reactivewm.controller.ControllerManager;
import org.reactivewm.exception.FailfastException;
import org.reactivewm.exception.ThreadException;
import org.reactivewm.executor.ISThreadExecutable;
import org.reactivewm.executor.ThreadExecutable;
import org.reactivewm.thread.ISThreadFactory;
import org.reactivewm.thread.is.ReactiveServiceThreadManager;

import com.google.common.util.concurrent.ListenableFuture;
import com.wm.app.b2b.server.ServiceThread;
import com.wm.app.b2b.server.ThreadManager;
import com.wm.control.ControlException;
import com.wm.data.IData;
import com.wm.data.IDataCursor;
import com.wm.data.IDataFactory;
import com.wm.data.IDataUtil;

/**
 * Main client class offering a facade of all ReactiveWM capabilities
 * 
 * @author Teiva Harsanyi
 * 
 */
public class ReactiveWMFacade {
	private static final Logger LOG = Logger.getLogger(ReactiveWMFacade.class);

	private static void checkPool(String pool) throws ThreadException, IllegalArgumentException {
		if (pool == null || "".equals(pool)) {
			throw new IllegalArgumentException("Pool name empty");
		}

		ReactiveServiceThreadManager manager = ReactiveServiceThreadManager.getInstance();
		if (!manager.isPoolExists(pool)) {
			throw new ThreadException("Pool " + pool + " not initialized");
		}
	}

	private static void checkPoolSize(int poolSize) throws IllegalArgumentException {
		if (poolSize < 0) {
			throw new IllegalArgumentException("Pool size must be positive");
		}
	}

	private static void checkTimeout(long timeout, TimeUnit timeUnit) throws IllegalArgumentException {
		if (timeout == 0 || timeUnit == null) {
			throw new IllegalArgumentException("Timeout not well defined");
		}
	}

	public static double getThreadActivity() {
		try {
			return ThreadManager.getThreadManagerImpl().getPoolMax() / ThreadManager.getThreadManagerImpl().getActive()
					* 100;
		} catch (ControlException e) {
			return -1;
		}
	}

	public static int getActiveThreads() {
		try {
			return ThreadManager.getThreadManagerImpl().getActive();
		} catch (ControlException e) {
			return -1;
		}
	}

	public static boolean createPool(String pool, int poolSize, boolean temporary, Date limit, boolean atomic) {
		return createPool(pool, poolSize, new ISThreadFactory(), temporary, limit, atomic);
	}

	public static boolean createPool(String pool, int poolSize, ThreadFactory factory, boolean temporary, Date limit, boolean atomic) {
		return createPool(pool, poolSize, factory, new ISThreadExecutable(), temporary, limit, atomic);
	}

	public static boolean createPool(String pool, int poolSize, ThreadFactory factory, ThreadExecutable executable,
			boolean temporary, Date limit, boolean atomic) {
		if (pool == null || "".equals(pool)) {
			throw new IllegalArgumentException("Pool name empty");
		}

		checkPoolSize(poolSize);

		ReactiveServiceThreadManager manager = ReactiveServiceThreadManager.getInstance();
		if (!manager.isPoolExists(pool)) {
			manager.createPool(pool, poolSize, factory, executable, temporary, limit, atomic);
			return true;
		}

		return false;
	}

	public static void wait(String pool, List<Future<IData>> futures, boolean failfast, long timeout, TimeUnit timeUnit)
			throws TimeoutException, ThreadException {
		if (futures == null || futures.size() == 0) {
			return;
		}

		checkPool(pool);
		checkTimeout(timeout, timeUnit);

		ReactiveServiceThreadManager manager = ReactiveServiceThreadManager.getInstance();

		try {
			manager.wait(futures, timeout, timeUnit, failfast);
		} catch (TimeoutException e) {
			throw new TimeoutException();
		} catch (FailfastException e) {
			e.printStackTrace();
		}
	}

	public static List<Future<IData>> parallelize(String pool, List<ServiceThread> serviceThreads, boolean failfast,
			long timeout, TimeUnit timeUnit) throws ThreadException, TimeoutException, FailfastException {
		if (serviceThreads == null || serviceThreads.size() == 0) {
			return null;
		}

		checkPool(pool);
		checkTimeout(timeout, timeUnit);

		ReactiveServiceThreadManager manager = ReactiveServiceThreadManager.getInstance();

		List<Future<IData>> futures = new ArrayList<Future<IData>>();
		for (ServiceThread st : serviceThreads) {
			Future<IData> future = manager.submit(pool, st);
			futures.add(future);
		}

		String controllerFailure = null;

		if (failfast) {
			try {
				controllerFailure = manager.addControllerFailure(pool, futures, serviceThreads);
			} catch (ThreadException e) {
				LOG.log(Level.ERROR, "Error while adding the failure controller: " + e.getMessage());
			}
		}

		try {
			manager.wait(futures, timeout, timeUnit, failfast);
		} finally {
			if (failfast) {
				ControllerManager controllerManager = ControllerManager.getInstance();
				controllerManager.removeController(controllerFailure);
			}
		}

		return futures;
	}

	public static ServiceThread createServiceThread(String pool, String serviceName, IData input, int threadPriority,
			boolean interruptable) {
		ReactiveServiceThreadManager manager = ReactiveServiceThreadManager.getInstance();
		return manager.createServiceThread(serviceName, input, threadPriority, interruptable);
	}
	
	public static ServiceThread createServiceThread(String pool, String serviceName, IData input, String ref,
			boolean interruptable) throws ThreadException {
		ReactiveServiceThreadManager manager = ReactiveServiceThreadManager.getInstance();
		return manager.createServiceThread(pool, serviceName, input, ref, interruptable);
	}

	public static ListenableFuture<IData> chain(String pool, ListenableFuture<IData> future, String service,
			IData input, Integer threadPriority, boolean merge, boolean interruptable) throws ThreadException {
		ReactiveServiceThreadManager manager = ReactiveServiceThreadManager.getInstance();
		return manager.chain(pool, future, service, input, threadPriority, merge, interruptable);
	}
	
	public static ListenableFuture<IData> chain(String pool, ListenableFuture<IData> future, String service,
			IData input, String ref, boolean merge, boolean interruptable) throws ThreadException {
		ReactiveServiceThreadManager manager = ReactiveServiceThreadManager.getInstance();
		return manager.chain(pool, future, service, input, ref, merge, interruptable);
	}

	public static ListenableFuture<IData> chain(String pool, ListenableFuture<IData> future, String service,
			IData input, Integer threadPriority, boolean merge, boolean interruptable, String errService,
			IData errInput, Integer errThreadPriority, boolean errInterruptable) throws ThreadException {
		ReactiveServiceThreadManager manager = ReactiveServiceThreadManager.getInstance();
		return manager.chain(pool, future, service, input, threadPriority, merge, interruptable, errService, errInput,
				errThreadPriority, errInterruptable);
	}
	
	public static ListenableFuture<IData> chain(String pool, ListenableFuture<IData> future, String service,
			IData input, String ref, boolean merge, boolean interruptable, String errService,
			IData errInput, String errRef, boolean errInterruptable) throws ThreadException {
		ReactiveServiceThreadManager manager = ReactiveServiceThreadManager.getInstance();
		return manager.chain(pool, future, service, input, ref, merge, interruptable, errService, errInput,
				errRef, errInterruptable);
	}

	public static ListenableFuture<IData> submit(String pool, String service, Integer threadPriority, IData input,
			boolean interruptable) throws ThreadException {
		checkPool(pool);

		ReactiveServiceThreadManager manager = ReactiveServiceThreadManager.getInstance();
		return manager.submit(pool, createServiceThread(pool, service, input, threadPriority, interruptable));
	}
	
	public static ListenableFuture<IData> submit(String pool, String service, String ref, IData input,
			boolean interruptable) throws ThreadException {
		checkPool(pool);

		ReactiveServiceThreadManager manager = ReactiveServiceThreadManager.getInstance();
		return manager.submit(pool, createServiceThread(pool, service, input, ref, interruptable));
	}

	public static void changePoolSize(String pool, int poolSize) throws ThreadException {
		checkPool(pool);
		checkPoolSize(poolSize);

		ReactiveServiceThreadManager manager = ReactiveServiceThreadManager.getInstance();
		manager.changePoolSize(pool, poolSize);
	}

	public static void startup(long frequency) {
		ReactiveServiceThreadManager manager = ReactiveServiceThreadManager.getInstance();
		manager.startup(frequency);
	}
	
	public static void shutdown() {
		ReactiveServiceThreadManager manager = ReactiveServiceThreadManager.getInstance();
		manager.shutdown();
	}

	public static void closePool(String pool, long timeout, TimeUnit timeUnit) throws ThreadException {
		try {
			checkPool(pool);
		} catch (Exception e) {
		}

		ReactiveServiceThreadManager manager = ReactiveServiceThreadManager.getInstance();
		manager.closePool(pool, timeout, timeUnit);
	}

	public static String introspect() {
		ReactiveServiceThreadManager manager = ReactiveServiceThreadManager.getInstance();
		return manager.introspect();
	}

	public static IData introspect(String pool) {
		ReactiveServiceThreadManager manager = ReactiveServiceThreadManager.getInstance();
		String s = manager.introspect(pool);

		if (s != null) {
			String[] n = s.split(";");
			if (n.length == 2) {
				IData id = IDataFactory.create();
				IDataCursor idCur = id.getCursor();

				IDataUtil.put(idCur, "size", n[0]);
				IDataUtil.put(idCur, "count", n[1]);

				idCur.destroy();
				return id;
			}
		}

		return null;
	}

}
