package org.reactivewm.thread.is;

import java.util.Comparator;

import com.wm.data.IData;

/**
 * Comparator of ListenableFutureTask
 * @author Teiva Harsanyi
 *
 */
public class ListenableFutureTaskComparator implements Comparator <ListenableFutureTask<IData>> {

	@Override
	public int compare(ListenableFutureTask<IData> o1, ListenableFutureTask<IData> o2) {
		if(o1 == null && o2 == null) {
			return 0;
		}
		if(o1 == null) {
			return 1;
		}
		if(o2 == null) {
			return -1;
		}
		
		int p1 = ((ReactiveServiceThread)(o1).getRunnable()).getThreadPriority();
		int p2 = ((ReactiveServiceThread)(o2).getRunnable()).getThreadPriority();
		
		return p1 < p2 ? 1 : (p1 == p2 ? 0 : -1);
	}

}
