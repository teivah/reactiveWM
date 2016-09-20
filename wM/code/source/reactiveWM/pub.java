package reactiveWM;

import com.wm.data.*;
import com.wm.util.Values;
import com.wm.app.b2b.server.Service;
import com.wm.app.b2b.server.ServiceException;
// --- <<IS-START-IMPORTS>> ---
import com.wm.control.ControlException;
import com.wm.app.b2b.server.Session;
import com.wm.app.b2b.server.ThreadManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.reactivewm.exception.ThreadException;
import org.reactivewm.facade.ReactiveWMFacade;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.ExecutionList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.wm.app.b2b.server.ServiceThread;
import com.wm.data.IData;
import com.wm.data.IDataFactory;
import com.wm.data.IDataUtil;
import com.wm.lang.flow.FlowException;
import com.wm.lang.ns.NSName;
// --- <<IS-END-IMPORTS>> ---

public final class pub

{
	// ---( internal utility methods )---

	final static pub _instance = new pub();

	static pub _newInstance() { return new pub(); }

	static pub _cast(Object o) { return (pub)o; }

	// ---( server methods )---




	public static final void chain (IData pipeline)
        throws ServiceException
	{
		// --- <<IS-START(chain)>> ---
		// @sigtype java 3.5
		// [i] record:1:required services
		// [i] - object:0:required future
		// [i] - field:0:required serviceName
		// [i] - record:0:optional input
		// [i] - field:0:optional priority {"1","2","3","4","5","6","7","8","9","10"}
		// [i] field:0:optional pool
		// [i] field:0:optional deepClone {"true","false"}
		// [o] object:1:required futures
		IDataCursor pipelineCur = pipeline.getCursor();
		
		try {
			IData[] services = IDataUtil.getIDataArray(pipelineCur, "services");
			
			if(services == null || services.length == 0) {
				return;
			}
			
			String pool = IDataUtil.getString(pipelineCur, "pool");
			boolean deepClone = Boolean.valueOf(IDataUtil.getString(pipelineCur, "deepClone"));
			
			IDataCursor servicesCur = null;
			
			List<ServiceThread> threads = new ArrayList<ServiceThread>();
			List<Future<IData>> futures = new ArrayList<Future<IData>>();
			
			for(int i=0; i<services.length; i++) {
				servicesCur = services[i].getCursor();
				
				String serviceName = IDataUtil.getString(servicesCur, "serviceName");
				String sPriority = IDataUtil.getString(servicesCur, "priority");
				IData input = IDataUtil.getIData(servicesCur, "input");
				@SuppressWarnings("unchecked")
				ListenableFuture<IData> future = (ListenableFuture<IData>)IDataUtil.get(servicesCur, "future");
				
				threads.add(ReactiveWMFacade.createServiceThread(serviceName, input, getPriority(sPriority)));
				
				try {
					if(deepClone) {
						futures.add(ReactiveWMFacade.chain(pool, future, serviceName, IDataUtil.deepClone(input), ReactiveWMFacade.MID_PRIORITY, false));
					} else {
						futures.add(ReactiveWMFacade.chain(pool, future, serviceName, input, ReactiveWMFacade.MID_PRIORITY, false));	
					}
				} catch (ThreadException e) {
					throw new ServiceException(e);
				}
			}
			
			if(futures == null || futures.size() != 0) {
				IDataUtil.put(pipelineCur, "futures", futures.toArray(new Future[futures.size()]));
			}
			
			if(servicesCur != null) {
				servicesCur.destroy();
			}
		} catch(Exception e) {
			throw new ServiceException(e);
		} finally {
			if(pipelineCur != null) {
				pipelineCur.destroy();
			}
		}
			
		// --- <<IS-END>> ---

                
	}



	public static final void changePoolSize (IData pipeline)
        throws ServiceException
	{
		// --- <<IS-START(changePoolSize)>> ---
		// @sigtype java 3.5
		// [i] field:0:required pool
		// [i] field:0:required newPoolSize
		IDataCursor pipelineCur = pipeline.getCursor();
		
		try {
			
			String pool = IDataUtil.getString(pipelineCur, "pool");
			
			String sPoolSize = IDataUtil.getString(pipelineCur, "newPoolSize");
			int poolSize = Integer.valueOf(sPoolSize);
			
			if(poolSize <= 0) {
				throw new ServiceException("maxThreads value must be positive");
			} else if(poolSize > MAX_THREADS) {
				poolSize = MAX_THREADS;
			}
			
			ReactiveWMFacade.changePoolSize(pool, poolSize);
			
		} catch(Exception e) {
			throw new ServiceException(e);
		} finally {
			if(pipelineCur != null) {
				pipelineCur.destroy();
			}
		}
		// --- <<IS-END>> ---

                
	}



	public static final void createPool (IData pipeline)
        throws ServiceException
	{
		// --- <<IS-START(createPool)>> ---
		// @sigtype java 3.5
		// [i] field:0:required pool
		// [i] field:0:required poolSize
		IDataCursor pipelineCur = pipeline.getCursor();
		
		try {
			
			String pool = IDataUtil.getString(pipelineCur, "pool");
			
			String sPoolSize = IDataUtil.getString(pipelineCur, "poolSize");
			int poolSize = Integer.valueOf(sPoolSize);
			
			if(poolSize <= 0) {
				throw new ServiceException("maxThreads value must be positive");
			} else if(poolSize > MAX_THREADS) {
				poolSize = MAX_THREADS;
			}
			
			ReactiveWMFacade.createPool(pool, poolSize);
			
		} catch(Exception e) {
			throw new ServiceException(e);
		} finally {
			if(pipelineCur != null) {
				pipelineCur.destroy();
			}
		}
		// --- <<IS-END>> ---

                
	}



	public static final void parallelize (IData pipeline)
        throws ServiceException
	{
		// --- <<IS-START(parallelize)>> ---
		// @sigtype java 3.5
		// [i] field:0:required pool
		// [i] record:1:required services
		// [i] - field:0:optional id
		// [i] - field:0:required serviceName
		// [i] - record:0:optional input
		// [i] - field:0:optional priority {"1","2","3","4","5","6","7","8","9","10"}
		// [i] field:0:optional failfast {"true","false"}
		// [i] field:0:optional deepClone {"true","false"}
		// [i] record:0:optional timeout
		// [i] - field:0:required duration
		// [i] - field:0:required timeUnit {"NANOSECONDS","MICROSECONDS","MILLISECONDS","SECONDS","MINUTES","HOURS","DAYS"}
		// [o] record:1:required services
		// [o] - field:0:optional id
		// [o] - field:0:required serviceName
		// [o] - record:0:optional input
		// [o] - record:0:required output
		// [o] - field:0:required isOnError
		// [o] - record:0:required error
		// [o] -- field:0:required errorMessage
		// [o] -- object:0:required exception
		// [o] field:0:required containsErrors
		IDataCursor pipelineCur = pipeline.getCursor();
		
		try {
			IData[] services = IDataUtil.getIDataArray(pipelineCur, "services");
			
			if(services == null || services.length == 0) {
				return;
			}
			
			String pool = IDataUtil.getString(pipelineCur, "pool");
			
			boolean deepClone = Boolean.valueOf(IDataUtil.getString(pipelineCur, "deepClone"));
			boolean failfast = Boolean.valueOf(IDataUtil.getString(pipelineCur, "failfast"));
			
			IData timeout = IDataUtil.getIData(pipelineCur, "timeout");			
			if(timeout == null) {
				throw new ServiceException("Timeout not set");
			}
			IDataCursor timeoutCur = timeout.getCursor();
			Long duration = parseDurationTimeout(timeoutCur);
			TimeUnit timeUnit = parseTimeUnitTimeout(timeoutCur);
			timeoutCur.destroy();
			
			IDataCursor servicesCur = null;
			
			List<ServiceThread> threads = new ArrayList<ServiceThread>();
			List<Integer> priorities = new ArrayList<Integer>();
			
			for(int i=0; i<services.length; i++) {
				servicesCur = services[i].getCursor();
				
				String serviceName = IDataUtil.getString(servicesCur, "serviceName");
				IData input = IDataUtil.getIData(servicesCur, "input");
				String sPriority = IDataUtil.getString(servicesCur, "priority");
				
				if(deepClone) {
					threads.add(ReactiveWMFacade.createServiceThread(serviceName, IDataUtil.deepClone(input), getPriority(sPriority)));
				} else {
					threads.add(ReactiveWMFacade.createServiceThread(serviceName, input, getPriority(sPriority)));
				}
				priorities.add(ReactiveWMFacade.MID_PRIORITY);
			}
			
			List<Future<IData>> futures = ReactiveWMFacade.parallelize(pool, threads, failfast, duration, timeUnit);
			
			if(futures == null) {
				return;
			}
			
			boolean containsErrors = false;			
			if(futures != null || futures.size() == 0) {
				int i = 0;
				for(Future<IData> future : futures) {
					servicesCur = services[i++].getCursor();
					IDataUtil.remove(servicesCur, "future");
					try {
						IDataUtil.put(servicesCur, "output", future.get());
					} catch(ExecutionException ee) {
						containsErrors = true;
						IDataUtil.put(servicesCur, "isOnError", "false");
						
						IData error = IDataFactory.create();
						IDataCursor errorCur = error.getCursor();						
						IDataUtil.put(errorCur, "errorMessage", ee.getCause().getMessage());
						IDataUtil.put(errorCur, "exception", ee.getCause());
						errorCur.destroy();
						
						IDataUtil.put(servicesCur, "error", error);
					}
				}
			}
			
			IDataUtil.put(pipelineCur, "containsErrors", Boolean.valueOf(containsErrors));
			
			if(servicesCur != null) {
				servicesCur.destroy();
			}
		} catch(TimeoutException e) {
			throw new ServiceException("Parallelize exception: timeout reached");
		} catch(Exception e) {
			throw new ServiceException("Parallelize exception: " + e.getMessage());
		} finally {
			if(pipelineCur != null) {
				pipelineCur.destroy();
			}
		}
		// --- <<IS-END>> ---

                
	}



	public static final void spawn (IData pipeline)
        throws ServiceException
	{
		// --- <<IS-START(spawn)>> ---
		// @sigtype java 3.5
		// [i] field:0:required $serviceName
		// [i] field:0:required $pool
		// [i] field:0:optional $priority {"1","2","3","4","5","6","7","8","9","10"}
		// [i] field:0:optional $deepClone {"true","false"}
		// [o] object:0:required future
		IDataCursor pipelineCur = pipeline.getCursor();
		
		try {
			String serviceName = IDataUtil.getString(pipelineCur, "$serviceName");
			String sPriority = IDataUtil.getString(pipelineCur, "$priority");
			String pool = IDataUtil.getString(pipelineCur, "$pool");
			boolean deepClone = Boolean.valueOf(IDataUtil.getString(pipelineCur, "$deepClone"));
			
			IDataUtil.remove(pipelineCur, "$serviceName");
			IDataUtil.remove(pipelineCur, "$priority");
			IDataUtil.remove(pipelineCur, "$pool");
			IDataUtil.remove(pipelineCur, "$deepClone");
			
			Future future = null;
			if(deepClone) {
				future = ReactiveWMFacade.submit(pool, serviceName, getPriority(sPriority), IDataUtil.deepClone(pipeline));	
			} else {
				future = ReactiveWMFacade.submit(pool, serviceName, getPriority(sPriority), pipeline);
			}
			
			IDataUtil.put(pipelineCur, "future", future);
		} catch(Exception e) {
			throw new ServiceException(e);
		} finally {
			if(pipelineCur != null) {
				pipelineCur.destroy();
			}
		}
			
		// --- <<IS-END>> ---

                
	}



	public static final void submit (IData pipeline)
        throws ServiceException
	{
		// --- <<IS-START(submit)>> ---
		// @sigtype java 3.5
		// [i] record:1:required services
		// [i] - field:0:required serviceName
		// [i] - record:0:optional input
		// [i] - field:0:optional priority {"1","2","3","4","5","6","7","8","9","10"}
		// [i] field:0:required pool
		// [i] field:0:optional deepClone {"true","false"}
		// [o] object:1:required futures
		IDataCursor pipelineCur = pipeline.getCursor();
		
		try {
			IData[] services = IDataUtil.getIDataArray(pipelineCur, "services");
			
			if(services == null || services.length == 0) {
				return;
			}
			
			String pool = IDataUtil.getString(pipelineCur, "pool");
		
			boolean deepClone = Boolean.valueOf(IDataUtil.getString(pipelineCur, "deepClone"));
						
			IDataCursor servicesCur = null;
			
			List<Future> futures = new ArrayList<Future>();
			
			for(int i=0; i<services.length; i++) {
				servicesCur = services[i].getCursor();
				
				String serviceName = IDataUtil.getString(servicesCur, "serviceName");
				IData input = IDataUtil.getIData(servicesCur, "input");
				String sPriority = IDataUtil.getString(servicesCur, "priority");
				
				if(deepClone) {
					futures.add(ReactiveWMFacade.submit(pool, serviceName, getPriority(sPriority), IDataUtil.deepClone(input)));	
				} else {
					futures.add(ReactiveWMFacade.submit(pool, serviceName, getPriority(sPriority), input));
				}
				
			}
			
			if(futures == null || futures.size() != 0) {
				IDataUtil.put(pipelineCur, "futures", futures.toArray(new Future[futures.size()]));
			}
			
			if(servicesCur != null) {
				servicesCur.destroy();
			}
		} catch(Exception e) {
			throw new ServiceException(e);
		} finally {
			if(pipelineCur != null) {
				pipelineCur.destroy();
			}
		}
			
		// --- <<IS-END>> ---

                
	}



	public static final void waitForCompletion (IData pipeline)
        throws ServiceException
	{
		// --- <<IS-START(waitForCompletion)>> ---
		// @sigtype java 3.5
		// [i] field:0:optional pool
		// [i] record:1:required services
		// [i] - field:0:optional id
		// [i] - object:0:required future
		// [i] record:0:required timeout
		// [i] - field:0:required duration
		// [i] - field:0:required timeUnit {"NANOSECONDS","MICROSECONDS","MILLISECONDS","SECONDS","MINUTES","HOURS","DAYS"}
		// [o] record:1:required services
		// [o] - field:0:optional id
		// [o] - record:0:required output
		// [o] - field:0:required isOnError
		// [o] - record:0:required error
		// [o] -- field:0:required errorMessage
		// [o] -- object:0:required exception
		// [o] field:0:required containsErrors
		IDataCursor pipelineCur = pipeline.getCursor();
		
		try {
			IData[] services = IDataUtil.getIDataArray(pipelineCur, "services");
			
			if(services == null || services.length == 0) {
				return;
			}
			
			String pool = IDataUtil.getString(pipelineCur, "pool");
			
			IData timeout = IDataUtil.getIData(pipelineCur, "timeout");			
			if(timeout == null) {
				throw new ServiceException("Timeout not set");
			}
			IDataCursor timeoutCur = timeout.getCursor();
			Long duration = parseDurationTimeout(timeoutCur);
			TimeUnit timeUnit = parseTimeUnitTimeout(timeoutCur);
			timeoutCur.destroy();
			
			IDataCursor servicesCur = null;			
			List<Future<IData>> futures = new ArrayList<Future<IData>>();
			
			for(int i=0; i<services.length; i++) {
				servicesCur = services[i].getCursor();
				
				Future<IData> future = (Future<IData>)IDataUtil.get(servicesCur, "future");
				futures.add(future);
			}
		
			ReactiveWMFacade.wait(pool, futures, duration, timeUnit);
			
			boolean containsErrors = false;
			if(futures != null || futures.size() == 0) {
				int i = 0;
				for(Future<IData> future : futures) {
					servicesCur = services[i++].getCursor();
					IDataUtil.remove(servicesCur, "future");
					try {
						IDataUtil.put(servicesCur, "output", future.get());
						IDataUtil.put(servicesCur, "isOnError", "false");
					} catch(ExecutionException ee) {
						containsErrors = true;
						IDataUtil.put(servicesCur, "isOnError", "false");
						
						IData error = IDataFactory.create();
						IDataCursor errorCur = error.getCursor();						
						IDataUtil.put(errorCur, "errorMessage", ee.getCause().getMessage());
						IDataUtil.put(errorCur, "exception", ee.getCause());						
						errorCur.destroy();
						
						IDataUtil.put(servicesCur, "error", error);
					}
				}
			}
								
			IDataUtil.put(pipelineCur, "containsErrors", Boolean.valueOf(containsErrors));
			
			if(servicesCur != null) {
				servicesCur.destroy();
			}
		} catch(TimeoutException e) {
			throw new ServiceException("Parallelize exception: Timeout reached");
		}
		catch(Exception e) {
			throw new ServiceException("Wait exception: " + e.getMessage());
		} finally {
			if(pipelineCur != null) {
				pipelineCur.destroy();
			}
		}
		// --- <<IS-END>> ---

                
	}

	// --- <<IS-START-SHARED>> ---
	
	private static int getPriority(String sPriority) throws ServiceException {
		int priority = ReactiveWMFacade.MID_PRIORITY;
		if(sPriority != null || !"".equals(sPriority)) {
			priority = Integer.valueOf(sPriority);
			if(priority < ReactiveWMFacade.MIN_PRIORITY) {
				throw new ServiceException("Priority must be higher than " + ReactiveWMFacade.MIN_PRIORITY);
			}
			if(priority > ReactiveWMFacade.MAX_PRIORITY) {
				throw new ServiceException("Priority must be lower than " + ReactiveWMFacade.MAX_PRIORITY);
			}
		}
		return priority;
	}
	
	private static long parseDurationTimeout(IDataCursor cursor) throws ServiceException {
		String s = IDataUtil.getString(cursor, "duration");
		if(s == null || "".equals(s)) {
			throw new ServiceException("Timeout not set");	
		}			
		return Long.valueOf(s);
	}
	
	private static TimeUnit parseTimeUnitTimeout(IDataCursor cursor) throws ServiceException {
		String s = IDataUtil.getString(cursor, "timeUnit");
		if(s == null || "".equals(s)) {
			throw new ServiceException("Timeout not set");	
		}
		return TimeUnit.valueOf(s);
	}
	
	private static final int MAX_THREADS = 100;
		
	// --- <<IS-END-SHARED>> ---
}

