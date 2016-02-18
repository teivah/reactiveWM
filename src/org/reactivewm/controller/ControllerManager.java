package org.reactivewm.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;

import com.wm.app.b2b.server.ServiceThread;
import com.wm.data.IData;

/**
 * Controller manager providing utility services
 * @author Teiva Harsanyi
 *
 */
public class ControllerManager {
	
	private static ControllerManager INSTANCE;
	private Map<String, List<ServiceThread>> threadsMap;
	private Map<String, List<Future<IData>>> futuresMap;
	
	private ControllerManager() {
		threadsMap = new HashMap<String, List<ServiceThread>>();
		futuresMap = new HashMap<String, List<Future<IData>>>();
	}
	
	public static ControllerManager getInstance() {
		if(INSTANCE == null) {
			INSTANCE = new ControllerManager();
		}
		
		return INSTANCE;
	}
	
	public String addController(List<ServiceThread> threads, List<Future<IData>> futures) {
		String uuid = UUID.randomUUID().toString();

		threadsMap.put(uuid, threads);
		futuresMap.put(uuid, futures);

		return uuid;
	}

	public List<ServiceThread> getThreads(String controller) {
		return threadsMap.get(controller);
	}
	
	public List<Future<IData>> getFutures(String controller) {
		return futuresMap.get(controller);
	}

	public void removeController(String controller) {
		threadsMap.remove(controller);
		futuresMap.remove(controller);
	}
}
