package org.reactivewm.controller;

import com.google.common.util.concurrent.FutureCallback;
import com.wm.data.IData;

/**
 * Internal controller abstract class
 * @author Teiva Harsani
 *
 * @param <V>
 */
public abstract class ControllerCallback<V> implements FutureCallback<IData> {

	private String controller;
	
	public ControllerCallback(String controller) {
		this.controller = controller;
	}
	
	String getController() {
		return controller;
	}
}
