package org.reactivewm.controller;

import java.util.List;
import java.util.concurrent.Future;

import org.reactivewm.thread.is.ListenableFutureTask;
import org.reactivewm.thread.is.ReactiveServiceThread;

import com.wm.app.b2b.server.ServiceThread;
import com.wm.data.IData;

/**
 * Implementation of a ControllerCallback to cancel waiting threads
 * @author Teiva Harsanyi
 *
 * @param <V>
 */
public class CancelController<V> extends ControllerCallback<V> {

	public CancelController(String controller) {
		super(controller);
	}

	@Override
	public void onSuccess(IData idata) {
	}
	
	@Override
	public void onFailure(Throwable thrown) {
		ControllerManager manager = ControllerManager.getInstance();
		
		List<ServiceThread> threads = manager.getThreads(getController());
		List<Future<IData>> futures = manager.getFutures(getController());
		
		int i=0;
		for (ServiceThread thread : threads) {
			ReactiveServiceThread ast = (ReactiveServiceThread)thread;
			ast.cancel();
			ListenableFutureTask<IData> task = (ListenableFutureTask<IData>)futures.get(i++);
			task.set(null);
			task.setException(thrown);
		}
	}
}
