package org.reactivewm.executor;

import org.reactivewm.thread.is.ListenableFutureTask;
import org.reactivewm.thread.is.ReactiveServiceThread;
import org.reactivewm.thread.is.ReactiveServiceThreadManager;

import com.google.common.util.concurrent.ListenableFuture;
import com.wm.app.b2b.server.ServiceThread;
import com.wm.app.b2b.server.ThreadManager;
import com.wm.data.IData;

public class ISThreadExecutable implements ThreadExecutable {

	private static final long THREAD_ALLOCATION_TIMEOUT = 600000l;

	@SuppressWarnings("unchecked")
	@Override
	public void execute(Runnable task, String poolName) {
		ListenableFutureTask<IData> ft = (ListenableFutureTask<IData>) task;
		if (!ft.isController()) {
			try {
				ReactiveServiceThread ast = (ReactiveServiceThread) ft.getRunnable();
				if (!ast.isCancelled()) {
					ISThreadPoolExecutor parent = ReactiveServiceThreadManager.getInstance().getParent(poolName);
					if(parent != null) {
						ListenableFuture<IData> future = ReactiveServiceThreadManager.getInstance().submit(parent, ast);
						ft.set(future.get());
					} else {
						ServiceThread st = (ServiceThread) ft.getRunnable();
						String id = ThreadManager.getThreadManagerImpl().runTarget(st, THREAD_ALLOCATION_TIMEOUT);
						ast.setId(id);
						ft.set(st.getIData());
					}
				} else {
					ft.set(null);
				}
			} catch (Exception e) {
				ft.setException(e);
				ft.set(null);
			}
		} else {
			task.run();
		}
	}

}
