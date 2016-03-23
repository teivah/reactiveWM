package org.reactivewm.thread.is;

import org.apache.log4j.Logger;
import org.reactivewm.executor.ISThreadPoolExecutor;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.ListenableFuture;
import com.wm.app.b2b.server.ServiceThread;
import com.wm.app.b2b.server.SessionManager;
import com.wm.data.IData;
import com.wm.data.IDataUtil;

/**
 * Async function used in the scope of asynchronous chaining
 * @author Teiva Harsanyi
 *
 */
public class ReactiveAsyncFunction implements AsyncFunction<IData, IData> {
	private static final Logger LOG = Logger.getLogger(ReactiveAsyncFunction.class);
	private ISThreadPoolExecutor executor;
	private String service;
	private boolean merge;
	private IData input;
	private int threadPriority;
	private String session;
	private boolean interruptable;
	
	public ReactiveAsyncFunction(ISThreadPoolExecutor executor, String service, IData input, int threadPriority, boolean merge, String session, boolean interruptable) {
		this.executor = executor;
		this.service = service;
		this.merge = merge;
		this.threadPriority = threadPriority;
		this.input = input;
		this.session = session;
		this.interruptable = interruptable;
	}

	@Override
	public ListenableFuture<IData> apply(IData response) throws Exception {
		LOG.debug("ReactiveAsyncFunction apply: " + threadPriority);
		if(merge) {
			if(input == null) {
				input = IDataUtil.clone(response);
			} else { 
				IDataUtil.merge(response, input);
			}
		}
		ReactiveServiceThreadManager manager = ReactiveServiceThreadManager.getInstance();
		ServiceThread st = manager.createServiceThread(service, input, SessionManager.create().getSession(session), threadPriority, interruptable);
		return manager.submit(executor, st);
	}
	
}
