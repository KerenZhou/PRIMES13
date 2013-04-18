package primes13;

import java.io.IOException;
import java.util.AbstractSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;

public class Tester {
    private static AbstractSet<Long> jSkipList = null;
    private static LazySkipList cSkipList = null;
    private static LazySkipList2 cSkipList2 = null;
    private static int useCSkipList = -1;


    private static class FindTester implements Runnable {
        int id;
        long element;
        long[] iter;

        public FindTester(int id, long[] iter) {
            this.id = id;
            this.element = id;
            this.iter = iter;
        }
        
        public void run() {
            while (!shouldRun) ;
            long iters = 0;
            
            for (; shouldRun; iters++) {
                element = (1103515245L * element + 12345L) & (RandomSource.m - 1);
                if(useCSkipList == 0) cSkipList.contains(element);
                else if(useCSkipList == 1) cSkipList2.contains(element);
                else jSkipList.contains(element);
            }
            iter[id] = iters;
        }
    }
    
    public static class FindArrays {
        public LazySkipList.Node[] prev = new LazySkipList.Node[LazySkipList.MAX_LEVEL + 1],
                succs = new LazySkipList.Node[LazySkipList.MAX_LEVEL + 1];
    }
    
    public static class MutateTester implements Runnable {
        int id;
        long element;
        long[] add;
        long[] rem;
        RandomSource rs;

        public MutateTester(int id, long[] add, long rem[]) {
            this.id = id;
            this.element = id;
            this.add = add;
            this.rem = rem;
        }
        
        public void run() {
            LazySkipList.Node[] elems = new LazySkipList.Node[2 * (LazySkipList.MAX_LEVEL + 1)];
            LazySkipList2.Node[] elems2 = new LazySkipList2.Node[2 * (LazySkipList2.MAX_LEVEL + 1)];
            int iters = 100;
            for(int i = 0; i < iters; i++) {
                long x = element;
                long start = System.nanoTime();
                for (int j = 0; j < 1000; j++) {
                    element = (1103515245L * element + 12345L) & (RandomSource.m - 1);
                    if(useCSkipList == 0) cSkipList.add(element, elems);
                    else if(useCSkipList == 1) cSkipList2.add(element, elems2);
                    else jSkipList.add(element);
                }
                add[id] += (System.nanoTime() - start) / iters;
                element = x;
                start = System.nanoTime();
                for (int j = 0; j < 1000; j++) {
                    element = (1103515245L * element + 12345L) & (RandomSource.m - 1);
                    if(useCSkipList == 0) cSkipList.remove(element, elems);
                    else if(useCSkipList == 1) cSkipList2.remove(element, elems2);
                    else jSkipList.remove(element);
                }
                rem[id] += (System.nanoTime() - start) / iters;
            }
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
    
    public static void main(String[] args) {
        double runTimeApprox = Double.parseDouble(args[0]);
        int numThreads = Integer.parseInt(args[1]);
        String className = "";
        if(args.length > 2) className = args[2];
        if(className.equalsIgnoreCase("stl_ts")) {
            jSkipList = new TreeSet<Long>();
        } else if(className.equals("stl_sl")) {
            jSkipList = new ConcurrentSkipListSet<Long>();
        } else if(className.equals("sl1")) {
            cSkipList = new LazySkipList();
            useCSkipList = 0;
        } else {
            cSkipList2 = new LazySkipList2();
            useCSkipList = 1;
        }
        
        LazySkipList.Node[] elems = new LazySkipList.Node[2 * (LazySkipList.MAX_LEVEL + 1)];
        LazySkipList2.Node[] elems2 = new LazySkipList2.Node[2 * (LazySkipList2.MAX_LEVEL + 1)];
        
        // Seed skiplist with starting elements
        RandomSource rs = new RandomSource(-5);
        
        int finalElem = 1000;
        int warmupElem = 1000000;
        for(int i = 0; i < warmupElem; i++) {
            long next = rs.next();
            if(useCSkipList == 0) { cSkipList.add(next, elems); cSkipList.contains(next); }
            else if(useCSkipList == 1) { cSkipList2.add(next, elems2); cSkipList2.contains(next); }
            else { jSkipList.add(next); jSkipList.contains(next); }
        }
        rs = new RandomSource(-5);
        for(int i = 0; i < warmupElem - finalElem; i++) {
            if(useCSkipList == 0) cSkipList.remove(rs.next(), elems);
            else if(useCSkipList == 1) cSkipList2.remove(rs.next(), elems2);
            else jSkipList.remove(rs.next());
        }
        Timer endTimer = new Timer(false);
        
        try {
            Runtime.getRuntime().exec("sudo set-cpus -n " + numThreads + " seq");
        } catch (IOException ex) {}
        
        // Initialize threads
        Thread[] workers = new Thread[numThreads];
        long[] iter = new long[numThreads];
        long[] add = new long[numThreads];
        long[] rem = new long[numThreads];
        for (int i = 0; i < numThreads; i++) {
            workers[i] = new Thread(new FindTester(i, iter));
            workers[i].start();
        }

        // Schedule stoping time
        endTimer.schedule(new KillTasks(), (long)(runTimeApprox * 60 * 1000));
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
        
        Thread thread = null;
        for(int i = 0; i < numThreads; i++) {
            thread = new Thread(new MutateTester(i, add, rem));
            thread.start();
        }
        
        try {
            thread.join();
        } catch (InterruptedException e) {}
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {}

        // Get total number of iterations finished
        long totalIters = 0;
        long addNS = 0, remNS = 0;
        for (int i = 0; i < numThreads; i++) {
            totalIters += iter[i];
            addNS += add[i];
            remNS += rem[i];
        }
        // Calculate rate (iterations / msec)
        double rate_find = 1.0 * totalIters / (endTime - startTime);
        double rate_add = 1.0 * numThreads * numThreads * 1000000000L / addNS;
        double rate_rem = 1.0 * numThreads * numThreads * 1000000000L / remNS;
;        // Log data points
        System.out.printf("%.4f,%.4f,%.4f", rate_find, rate_add, rate_rem);

        // Stop timer so program can exit
        endTimer.purge();
        endTimer.cancel();
    }
}
