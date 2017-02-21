package com.shugaliy;

import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Yuriy Shugaliy
 *
 * Assuming that the priority values can be in the range of 0 and 10.
 * In other case, when the max value of priority is not defined,
 * it is possible to determine it as the max value among current input queues priorities.
 *
 * Created on 20.02.2017
 */
public class StringCombiner extends Combiner<String> {

    private static final double MAX_PRIORITY_VALUE = 10;
    private static Logger log = Logger.getLogger(StringCombiner.class.getName());

    private final ConcurrentHashMap<Integer, QueueReader> queueReaders = new ConcurrentHashMap<>();

    public StringCombiner(SynchronousQueue<String> outputQueue) {
        super(outputQueue);
    }

    @Override
    public void addInputQueue(BlockingQueue<String> queue, double priority, long isEmptyTimeout, TimeUnit timeUnit) throws CombinerException {
        if (queue == null || timeUnit == null) {
            throw new IllegalArgumentException("'queue' and 'timeUnit' values can not be null");
        }
        if (priority < 0 || priority > MAX_PRIORITY_VALUE) {
            throw new IllegalArgumentException("'priority' value can not be less than 0 or bigger than " + MAX_PRIORITY_VALUE);
        }
        if (isEmptyTimeout < 0) {
            throw new IllegalArgumentException("'isEmptyTimeout' value can not be negative");
        }
        if (queueReaders.containsKey(queue.hashCode())) {
            throw new CombinerException("Combiner has already contained this queue");
        }
        QueueReader queueReader = new QueueReader(queue, priority, isEmptyTimeout, timeUnit);
        CompletableFuture.runAsync(queueReader);
        queueReaders.put(queue.hashCode(), queueReader);
    }

    @Override
    public void removeInputQueue(BlockingQueue<String> queue) throws CombinerException {
        QueueReader queueReader = queueReaders.remove(queue.hashCode());
        if (queueReader != null) {
            queueReader.stopRead();
        } else {
            throw new CombinerException("Combiner does not contain such queue");
        }
    }

    @Override
    public boolean hasInputQueue(BlockingQueue<String> queue) {
        if (queue == null) {
            throw new IllegalArgumentException("Input queue can not be null");
        }
        return queueReaders.containsKey(queue.hashCode());
    }

    private class QueueReader extends Thread {
        private volatile boolean running = true;
        private BlockingQueue<String> queue;
        private double priority;
        private long isEmptyTimeout;
        private TimeUnit timeUnit;

        QueueReader(BlockingQueue<String> queue, double priority, long isEmptyTimeout, TimeUnit timeUnit) {
            this.setName("STRING COMBINER THREAD-" + getId());
            this.queue = queue;
            this.priority = priority;
            this.isEmptyTimeout = isEmptyTimeout;
            this.timeUnit = timeUnit;
        }

        @Override
        public void run() {
            while (running) {
                try {
                    String value = queue.poll(isEmptyTimeout, timeUnit);
                    if (value == null) {
                        queueReaders.remove(queue.hashCode());
                        break;
                    }
                    outputQueue.put(value);
                    Thread.sleep((int) (MAX_PRIORITY_VALUE / priority * 10));
                } catch (InterruptedException e) {
                    log.log(Level.INFO, "Queue reader thread with priority [{0}] was interrupted", priority);
                    break;
                }
            }
        }

        void stopRead() {
            running = false;
        }
    }
}
