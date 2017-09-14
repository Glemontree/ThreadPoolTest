package com.glemontree.threadpool;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class ThreadPool {
    private static final int WORKER_NUM = 5;
    private int mWorkerNum;
    private static WorkThread[] workThreads;
    private static volatile int finished_task = 0;
    private static List<Runnable> taskQueue = new LinkedList<>();
    private static ThreadPool threadPool;

    private ThreadPool() {
        this(5);
    }

    private ThreadPool(int worker_num) {
        this.mWorkerNum = worker_num;
        workThreads = new WorkThread[worker_num];
        for (int i = 0; i < worker_num; i++) {
            workThreads[i] = new WorkThread();
            workThreads[i].start();
        }
    }

    public static ThreadPool getThreadPool() {
        if (threadPool == null) {
            // 如果不同线程监视同一个实例或者不同的实例对象都会等待
            threadPool = getThreadPool(ThreadPool.WORKER_NUM);
        }
        return threadPool;
    }

    public static ThreadPool getThreadPool(int worker_num) {
        if (worker_num <= 0) {
            worker_num = ThreadPool.WORKER_NUM;
        }
        if (threadPool == null) {
            synchronized (ThreadPool.class) {
                if (threadPool == null) {
                    threadPool = new ThreadPool(worker_num);
                }
            }
        }
        return threadPool;
    }

    public void execute(Runnable task) {
        synchronized (taskQueue) {
            taskQueue.add(task);
            taskQueue.notify();
        }
    }

    public void execute(Runnable[] task) {
        synchronized (taskQueue) {
            for (Runnable t : task) {
                taskQueue.add(t);
            }
            taskQueue.notify();
        }
    }

    public void execute(List<Runnable> task) {
        synchronized (taskQueue) {
            for (Runnable t : task) {
                taskQueue.add(t);
            }
            taskQueue.notify();
        }
    }

    public void destroy() {
        synchronized (taskQueue) {
            while (!taskQueue.isEmpty()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            for (int i = 0; i < mWorkerNum; i++) {
                workThreads[i].stopWorker();
                workThreads[i] = null;
            }
            threadPool = null;
            taskQueue.clear();
        }
    }

    public int getWorkThreadNumber() {
        return mWorkerNum;
    }

    public int getFinishedTaskNumber() {
        return finished_task;
    }

    public int getWaitTaskNumber() {
        return taskQueue.size();
    }

    @Override
    public String toString() {
        return "ThreadPool{" +
                "workThreads=" + Arrays.toString(workThreads) +
                ", taskQueue=" + taskQueue +
                '}';
    }

    private class WorkThread extends Thread {
        private boolean isRunning = true;
        @Override
        public void run() {
            Runnable r = null;
            while (isRunning) {
                synchronized (taskQueue) {
                    while (isRunning && taskQueue.isEmpty()) {
                        try {
                            taskQueue.wait(20);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    if (!taskQueue.isEmpty()) {
                        r = taskQueue.remove(0);
                    }
                }
                if (r != null) {
                    try {
                        r.run();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                finished_task++;
                r = null;
            }
        }

        public void stopWorker() {
            isRunning = false;
        }
    }
}
