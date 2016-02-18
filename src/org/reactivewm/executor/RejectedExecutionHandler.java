package org.reactivewm.executor;

/**
 * Rejected execution handler
 * @author Teiva Harsanyi
 *
 */
public interface RejectedExecutionHandler {
    void rejectedExecution(Runnable r, ISThreadPoolExecutor executor);
}