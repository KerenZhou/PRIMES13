package primes13;

import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;

public class TesterSTL_TS {
    private static TreeSet<Long> skipList = new TreeSet<Long>();

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
                skipList.contains(element);
            }
            iter[id] = iters;
        }
    }
    
    private static class MutateTester implements Runnable {
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
            int iters = 100;
            for(int i = 0; i < iters; i++) {
                long x = element;
                long start = System.nanoTime();
                for (int j = 0; j < 1000; j++) {
                    element = (1103515245L * element + 12345L) & (RandomSource.m - 1);
                    skipList.add(element);
                }
                add[id] += (System.nanoTime() - start) / iters;
                element = x;
                start = System.nanoTime();
                for (int j = 0; j < 1000; j++) {
                    element = (1103515245L * element + 12345L) & (RandomSource.m - 1);
                    skipList.remove(element);
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
        int numThreads = 1;
        // Seed skiplist with starting elements
        RandomSource rs = new RandomSource(-5);
        for(int i = 0; i < 1000; i++) {
            skipList.add(rs.next());
        }
        Timer endTimer = new Timer(false);
            
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

        for(int i = 0; i < 999000; i++) {
            skipList.add(rs.next());
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

        // Log data points
        System.out.printf("%.4f,%.4f,%.4f", rate_find, rate_add, rate_rem);

        // Stop timer so program can exit
        endTimer.purge();
        endTimer.cancel();
    }
}
