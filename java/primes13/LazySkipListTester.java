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

public class LazySkipListTester {
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
            
            for (; shouldRun; iters++) {
                element = (1103515245L * element + 12345L) & (RandomSource.m - 1);
                skipList.add(element);
                skipList.contains(element);
                skipList.remove(element);
            }
            iter[id] = iters;
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
        for(int i = 0; i < 1000000; i++) {
            skipList.add(rs.next());
        }
        // Initialize plots
        XYSeriesCollection rate_d = new XYSeriesCollection();
        XYSeries rate = new XYSeries("Operations/ms");
        rate_d.addSeries(rate);
        JFreeChart rate_c = createChart(rate_d, "Rate");
        
        XYSeriesCollection speedup_d = new XYSeriesCollection();
        XYSeries speedup = new XYSeries("Speedup");
        speedup_d.addSeries(speedup);
        JFreeChart speedup_c = createChart(speedup_d, "Speedup");
        
        XYSeriesCollection efficiency_d = new XYSeriesCollection();
        XYSeries efficiency = new XYSeries("Efficiency");
        efficiency_d.addSeries(efficiency);
        JFreeChart efficiency_c = createChart(efficiency_d, "Efficiency");
        
        double rate0 = 0; // Starting rate
        Timer endTimer = new Timer(false);
        
        System.out.println("{");
        while (numThreads + socketSize <= maxThreads) {
            numThreads += socketSize;
            try {
                Runtime.getRuntime().exec("set-cpus -n " + numThreads + " seq");
            } catch (IOException ex) {}
            
            // Initialize threads
            Thread[] workers = new Thread[numThreads];
            long[] iter = new long[numThreads];
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
            // Get total number of iterations finished
            long totalIters = 0;
            for (int i = 0; i < numThreads; i++) {
                totalIters += iter[i];
            }
            // Calculate rate (iterations / msec)
            double rate_ = 1.0 * totalIters / (endTime - startTime);
            // If this is first runthrough, set initial rate
            if(numThreads == socketSize) rate0 = rate_;
            // Calculate speedup
            double speedup_ = rate_ / rate0;
            // Calculate efficiency
            double efficiency_ = speedup_ / numThreads;
            
            // Add data points to plot
            rate.add(numThreads, rate_);
            speedup.add(numThreads, speedup_);
            efficiency.add(numThreads, efficiency_);
            
            // Log data points
            System.out.printf("{ %d, %.4f, %.4f, %.4f }", numThreads, rate_, speedup_, efficiency_);
            if(numThreads + socketSize <= maxThreads) System.out.print(",");
            System.out.println();
        }
        // Stop timer so program can exit
        endTimer.purge();
        endTimer.cancel();
        
        System.out.print("}");
        
        // Save charts
        try {
            ChartUtilities.saveChartAsPNG(
                    new File(computername + "-rate-" + date + ".png"),
                    rate_c, 1280, 720);
            ChartUtilities.saveChartAsPNG(
                    new File(computername + "-speedup-" + date + ".png"),
                    speedup_c, 1280, 720);
            ChartUtilities.saveChartAsPNG(
                    new File(computername + "-efficiency-" + date + ".png"),
                    efficiency_c, 1280, 720);
        } catch(Exception ex) {}
        // Re-enable all cpus
        try {
            Runtime.getRuntime().exec("set-cpus all");
        } catch (IOException ex) {}
        
        // Flush everything to file
        System.out.flush();
        System.out.close();
    }
}