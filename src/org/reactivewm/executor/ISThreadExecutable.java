package org.reactivewm.executor;

import org.reactivewm.thread.is.ListenableFutureTask;
import org.reactivewm.thread.is.ReactiveServiceThread;

import com.wm.app.b2b.server.ServiceThread;
import com.wm.app.b2b.server.ThreadManager;
import com.wm.data.IData;

public class ISThreadExecutable implements ThreadExecutable {

	private static final long THREAD_ALLOCATION_TIMEOUT = 600000l;

	@SuppressWarnings("unchecked")
	@Override
	public void execute(Runnable task) {
		ListenableFutureTask<IData> ft = (ListenableFutureTask<IData>) task;
		if (!ft.isController()) {
			try {
				ReactiveServiceThread ast = (ReactiveServiceThread) ft
						.getRunnable();
				if (!ast.isCancelled()) {
					ServiceThread st = (ServiceThread) ft.getRunnable();
					String id = ThreadManager.getThreadManagerImpl().runTarget(
							st, THREAD_ALLOCATION_TIMEOUT);
					ast.setId(id);
					ft.set(st.getIData());
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
