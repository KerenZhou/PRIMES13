package primes13;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class LazySkipListTester2 {
    final static int[] TOM = {6, 48};
    final static int[] BEN = {10, 80};
    final static int maxThreads = Runtime.getRuntime().availableProcessors();
    static int socketSize;
    static int numThreads = 0;
    static int runTimes;
    private static LazySkipList skipList = new LazySkipList();

    private static class TestThread implements Runnable {
        int id;
        long element;
        long[] iter;
        RandomSource rs;

        public TestThread(int id, long[] iter) {
            this.id = id;
            this.element = id;
            this.iter = iter;
        }
        
        public void run() {
            while (!shouldRun) ;
            long iters = 0;
            long x = -5 >>> 1;
            
            for (; shouldRun; iters++) {
                x = (1103515245L * x + 12345L) & (RandomSource.m - 1);
                skipList.contains(x);
            }
            iter[id] = iters;
        }
    }
    
    private static class TestThread2 implements Runnable {
        int id;
        long element;
        long[] add;
        long[] rem;
        RandomSource rs;

        public TestThread2(int id, long[] add, long rem[]) {
            this.id = id;
            this.element = id;
            this.add = add;
            this.rem = rem;
        }
        
        public void run() {
            while (!shouldRun) ;
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
                    skipList.add(element);
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

    private static JFreeChart createChart(final XYDataset dataset, String title) {
        final JFreeChart chart = ChartFactory.createScatterPlot(
                title, // chart title
                "X (threads)", // x axis label
                "Y", // y axis label
                dataset, // data
                PlotOrientation.VERTICAL,
                true, // include legend
                true, // tooltips
                false // urls
                );
        XYPlot plot = (XYPlot) chart.getPlot();
        
        plot.getDomainAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        plot.getDomainAxis().setAutoRange(true);
        
        ((NumberAxis) plot.getRangeAxis()).setAutoRangeIncludesZero(true);
        plot.getRangeAxis().setAutoRange(true);
        
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        plot.setRenderer(renderer);
        
        plot.setBackgroundPaint(Color.white);
        plot.setRangeGridlinePaint(Color.black);
        
        return chart;
    }
    
    static double runTimeApprox = 1;
    public static void main(String[] args) {
        if(args.length > 0) {
            if(args[0].equalsIgnoreCase("-h")) {
                System.out.println("Usage: java -jar PRIMES13.jar [Time per step] [Step]");
            } else {
                try {
                    System.out.println(args[0]);
                    double rt = Double.parseDouble(args[0]);
                    runTimeApprox = rt;
                } catch(Exception ex) {}
            }
        }
        int size = 1;
        try {
            size = Integer.parseInt(args[1]);
        } catch(Exception ex) {
            if (maxThreads == TOM[1]) {
                size = TOM[0];
            } else if (maxThreads == BEN[1]) {
                size = BEN[0];
            }
            size = 1;
        } finally {
            socketSize = size;
        }
        // Create logfile
        String computername = "unknown";
        PrintStream os;
        String date = new SimpleDateFormat("yyyyMMdd-HH.mm").format(Calendar.getInstance().getTime()).toString();
        try {
            computername = InetAddress.getLocalHost().getHostName();
        } catch(Exception ex) {}
        try {
            os = new PrintStream(new File("log-" + computername + "-" + (int) (runTimeApprox * 60) + "s-" + date + ".log"));
            System.setOut(os);
        } catch(Exception ex) {}
        // Seed skiplist with starting elements
        RandomSource rs = new RandomSource(-5);
        for(int i = 0; i < 10000000; i++) {
            skipList.add(rs.next());
        }
        double rate0 = 0; // Starting rate
        Timer endTimer = new Timer(false);
        
        System.out.println("{");
        while (numThreads + socketSize <= maxThreads) {
            numThreads += socketSize;
            try {
                Runtime.getRuntime().exec("sudo /home/am6/mpdev/bin/set-cpus 0-" + (numThreads - 1));
            } catch (IOException ex) {}
            
            // Initialize threads
            Thread[] workers = new Thread[numThreads];
            long[] iter = new long[numThreads];
            long[] add = new long[numThreads];
            long[] rem = new long[numThreads];
            for (int i = 0; i < numThreads; i++) {
                workers[i] = new Thread(new TestThread(i, iter));
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
            
            for(int i = 0; i < numThreads; i++) {
                workers[i] = new Thread(new TestThread2(i, add, rem));
                workers[i].start();
            }
            
            // Wait until all threads are done
            for (int i = 0; i < numThreads; i++) {
                try {
                    workers[i].join();
                } catch (InterruptedException e) {}
            }
            
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
            double rate_add = 1.0 * numThreads * 1000 / (addNS / 1000000L);
            double rate_rem = 1.0 * numThreads * 1000 / (remNS / 1000000L);
            
            // Log data points
            System.out.printf("{ %d, %.4f, %.4f, %.4f }", numThreads / socketSize, rate_find, rate_add, rate_rem);
            if(numThreads + socketSize <= maxThreads) System.out.print(",");
            System.out.println();
            System.gc();
        }
        // Stop timer so program can exit
        endTimer.purge();
        endTimer.cancel();
        
        System.out.print("}");
        // Flush everything to file
        System.out.flush();
        System.out.close();
    }
}
