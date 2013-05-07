package primes13;

import java.io.IOException;
import java.util.*;

public class Tester {
    private static LazySkipList cSkipList = null;
    private static Timer endTimer = new Timer(false);
    
    public static class RunTester implements Runnable {
        int id;
        long element;
        long[] iters;
        RandomSource rs;

        public RunTester(int id, long[] iters) {
            this.id = id;
            this.element = id;
            this.iters = iters;
        }
        
        public void run() {
            LazySkipList.Node[] elems = new LazySkipList.Node[2 * (LazySkipList.MAX_LEVEL + 1)];
            while(!shouldRun) ;
            long it = 0;
            for(; shouldRun; it++) {
                element = (1103515245L * element + 12345L) & (RandomSource.m - 1);
                cSkipList.add(element, elems);
                cSkipList.remove(element, elems);
            }
            iters[id] = it;
        }
    }
    
    static volatile boolean shouldRun = false;
    static long startTime = 0, endTime;
    
    private static class KillTasks extends TimerTask {
        public void run() {
            shouldRun = false;
            endTime = System.nanoTime() / 1000000L;
        }
    }
    
    public static double test(int elements, int numThreads) {
        System.gc();
        try {
            Thread.sleep(1000L);
        } catch(Exception ex) {
        }
        LazySkipList.Node[] elems = new LazySkipList.Node[2 * (LazySkipList.MAX_LEVEL + 1)];
        cSkipList = new LazySkipList();

        //LazySkipList.useAllocHack = false;

        RandomSource rs = new RandomSource();
        for(int i = 0; i < elements; i++) {
            cSkipList.add(rs.next(), elems);
        }

        //LazySkipList.useAllocHack = true;

        // Initialize threads
        Thread[] workers = new Thread[numThreads];
        long[] iter = new long[numThreads];
        for (int i = 0; i < numThreads; i++) {
            workers[i] = new Thread(new RunTester(i, iter));
            workers[i].start();
        }
        try {
            Thread.sleep(1000L);
        } catch(Exception ex) {
        }
        // Schedule stoping time
        endTimer.schedule(new KillTasks(), (long)(0.5 * 60 * 1000));
        // Start iteration
        shouldRun = true;
        // Mark start
        startTime = System.nanoTime() / 1000000L;
        // Wait until all threads are done
        for (int i = 0; i < numThreads; i++) {
            try {
                workers[i].join();
            } catch (InterruptedException e) {}
        }
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {}

        // Get total number of iterations finished
        long totalIters = 0;
        for (int i = 0; i < numThreads; i++) {
            totalIters += iter[i];
        }
        return 1.0 * totalIters / (endTime - startTime);
    }

    public static void main(String[] args) {
        try {
            Thread.sleep(1000L);
        } catch(Exception ex) {
        }
        int numThreads = Integer.parseInt(args[1]);
        int numElems = Integer.parseInt(args[2]);
        String className = "";
        cSkipList = new LazySkipList();
        
        LazySkipList.Node[] elems = new LazySkipList.Node[2 * (LazySkipList.MAX_LEVEL + 1)];
        
        // Seed skiplist with starting elements
        RandomSource rs = new RandomSource(-5);
        
        //LazySkipList.useAllocHack = false;
        int warmupElem = 1000000;
        for(int i = 0; i < warmupElem; i++) {
            long next = rs.next();
            cSkipList.add(next, elems);
            cSkipList.remove(next, elems);
        }
        
        for(int i = 0; i < 1; i++) {
            System.out.println(numThreads + " " + numElems + " " + test(numElems, numThreads));
            numElems *= 10;
        }

        // Stop timer so program can exit
        endTimer.purge();
        endTimer.cancel();
    }
}
