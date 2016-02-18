package org.reactivewm.thread.is;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.reactivewm.executor.ISThreadPoolExecutor;

import com.google.common.util.concurrent.FutureCallback;
import com.wm.app.b2b.server.ServiceThread;
import com.wm.app.b2b.server.SessionManager;
import com.wm.data.IData;
import com.wm.data.IDataCursor;
import com.wm.data.IDataFactory;
import com.wm.data.IDataUtil;

public class FailureCallback<V> implements FutureCallback<IData> {

	private static final Logger LOG = Logger.getLogger(FailureCallback.class);

	private ISThreadPoolExecutor executor;
	private String service;
	private IData input;
	private int threadPriority;
	private String session;
	private boolean interruptable;

	public FailureCallback(ISThreadPoolExecutor executor, String service,
			IData input, int threadPriority, String session, boolean interruptable) {
		this.executor = executor;
		this.service = service;
		this.threadPriority = threadPriority;
		this.input = input;
		this.session = session;
		this.interruptable = interruptable;
	}

	@Override
	public void onFailure(Throwable arg0) {
		ReactiveServiceThreadManager manager = ReactiveServiceThreadManager
				.getInstance();
		try {
			
			IDataCursor inputCur = input.getCursor();
			
			IData lastError = IDataFactory.create();
			IDataUtil.put(inputCur, "lastError", lastError);
			IDataCursor lastErrorCur = lastError.getCursor();
			
			IDataUtil.put(lastErrorCur, "error", arg0.getMessage());
			IDataUtil.put(lastErrorCur, "errorType", arg0.getClass());
			
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			arg0.printStackTrace(pw);
			IDataUtil.put(lastErrorCur, "errorDump", sw.toString());
			sw.close();
			pw.close();
			
			lastErrorCur.destroy();
			inputCur.destroy();
			
			
			ServiceThread st = manager.createServiceThread(service, input, SessionManager.create().getSession(session), threadPriority, interruptable);
			manager.submit(executor, st);
		} catch (Exception e) {
			LOG.log(Level.ERROR,
					"Error while adding executing failure callback: "
							+ e.getMessage());
		}
	}

	@Override
	public void onSuccess(IData arg0) {
	}
}
