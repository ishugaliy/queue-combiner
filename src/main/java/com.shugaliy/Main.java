package com.shugaliy;

import java.util.concurrent.*;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        new Main().start();
    }

    private void start() throws InterruptedException {
        BlockingQueue<String> higherInputQueue = new LinkedBlockingQueue<>();
        BlockingQueue<String> mediumInputQueue = new LinkedBlockingQueue<>();
        BlockingQueue<String> mediumInputQueue2 = new LinkedBlockingQueue<>();
        BlockingQueue<String> lowerInputQueue = new LinkedBlockingQueue<>();

        for (int i = 0; i < 1000; i++) {
            lowerInputQueue.put("LQ:" + (i + 1));
            mediumInputQueue.put("MQ:" + (i + 1));
            mediumInputQueue2.put("MQ_2:" + (i + 1));
            higherInputQueue.put("HQ:" + (i + 1));
        }

        SynchronousQueue<String> outputQueue = new SynchronousQueue<>();
        Combiner<String> combiner = new StringCombiner(outputQueue);
        try {
            combiner.addInputQueue(lowerInputQueue, 1, 1, TimeUnit.SECONDS);
            combiner.addInputQueue(higherInputQueue, 7, 1, TimeUnit.SECONDS);
            combiner.addInputQueue(mediumInputQueue, 2, 1, TimeUnit.SECONDS);
        } catch (Combiner.CombinerException e) {
            e.printStackTrace();
        }

        int counter = 1;
        while (true) {
            try {
                String value = outputQueue.take();
                if (value == null) {
                    break;
                }
                if (value.equals("LQ:2")) {
                    combiner.removeInputQueue(mediumInputQueue);
                    System.out.println(">>>>REMOVED MQ<<<<");
                    combiner.addInputQueue(mediumInputQueue2, 2, 1, TimeUnit.SECONDS);
                    System.out.println(">>>>ADDED MQ_2<<<<");
                }
                System.out.println("GOT value = " + value + ", counter = " + counter++);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
