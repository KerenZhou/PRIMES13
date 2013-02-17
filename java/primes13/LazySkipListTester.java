package primes13;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.experimental.chart.renderer.xy.XYSmoothLineAndShapeRenderer;

public class LazySkipListTester {

    final static int[] TOM = {6, 48};
    final static int[] BEN = {10, 80};
    final static int maxThreads = Runtime.getRuntime().availableProcessors();
    final static int socketSize;
    static int numThreads = 0;
    static int runTimes;
    private static LazySkipList skipList = new LazySkipList();
    static boolean DEBUG_TIME = false;
    
    static {
        int size;
        if (maxThreads == TOM[1]) {
            size = TOM[0];
        } else if (maxThreads == BEN[1]) {
            size = BEN[0];
        } else {
            size = 1;
        }
        size = 1;
        socketSize = size;
    }

    private static class TestThread implements Runnable {

        int id;
        long[] add, find, delete, iter;
        RandomSource rs;

        public TestThread(int id, long[] add, long[] find, long[] delete, long[] iter) {
            this.id = id;
            this.add = add;
            this.find = find;
            this.delete = delete;
            this.iter = iter;
            this.rs = new RandomSource(id);
        }
        
        public void run() {
            while (!shouldRun) ;
            long nanoTime = 0, total = 0, iters = 0;

            for (int i = 0; shouldRun; i++) {
                long element = rs.next();
                if(DEBUG_TIME) {
                    nanoTime = System.nanoTime();
                }
                skipList.add(element);
                if(DEBUG_TIME) {
                    total = System.nanoTime() - nanoTime;
                    add[id] += total;
                    nanoTime = System.nanoTime();
                }
                skipList.contains(element);
                if(DEBUG_TIME) {
                    total = System.nanoTime() - nanoTime;
                    find[id] += total;
                    nanoTime = System.nanoTime();
                }
                skipList.remove(element);
                if(DEBUG_TIME) {
                    total = System.nanoTime() - nanoTime;
                    delete[id] += total;
                }
                iters++;
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
        plot.getRangeAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        plot.setBackgroundPaint(Color.white);
        plot.setRangeGridlinePaint(Color.black);
        plot.setRenderer(renderer);
        return chart;
    }
    
    static double runTimeApprox = 1;
    public static void main(String[] args) {
        if(args.length > 0) {
            try {
                System.out.println(args[0]);
                double rt = Double.parseDouble(args[0]);
                runTimeApprox = rt;
            } catch(Exception ex) {}
        }
        String computername = "unknown";
        PrintStream os = null;
        String date = new SimpleDateFormat("yyyyMMdd-HH.mm").format(Calendar.getInstance().getTime()).toString();
        try {
            computername = InetAddress.getLocalHost().getHostName();
        } catch(Exception ex) {}
        try {
            os = new PrintStream(new File("log-" + computername + "-" + runTimeApprox + "min-" + date + ".log"));
            System.setOut(os);
        } catch(Exception ex) {}
        
        RandomSource rs = new RandomSource(-5);
        
        for(int i = 0; i < 1000000; i++) {
            skipList.add(rs.next());
        }
        
        System.out.print("\n\n");
        XYSeriesCollection rate_d = new XYSeriesCollection();
        XYSeries rate = new XYSeries("Operations/Second");
        rate_d.addSeries(rate);
        XYSeriesCollection speedup_d = new XYSeriesCollection();
        XYSeries speedup = new XYSeries("Speedup");
        speedup_d.addSeries(speedup);
        XYSeriesCollection efficiency_d = new XYSeriesCollection();
        XYSeries efficiency = new XYSeries("Efficiency");
        efficiency_d.addSeries(efficiency);
        JFreeChart rate_c = createChart(rate_d, "Rate");
        JFreeChart speedup_c = createChart(speedup_d, "Speedup");
        JFreeChart efficiency_c = createChart(efficiency_d, "Efficiency");
        double rate0 = 0;
        while (numThreads < maxThreads) {
            numThreads += socketSize;
            try {
                Runtime.getRuntime().exec("set-cpus -n " + numThreads + " seq");
            } catch (IOException ex) {}
            System.out.println("Starting with: " + numThreads + " threads.");
            Thread[] workers = new Thread[numThreads];
            long[] add = new long[numThreads], find = new long[numThreads], delete = new long[numThreads], iter = new long[numThreads];
            for (int i = 0; i < numThreads; i++) {
                workers[i] = new Thread(new TestThread(i, add, find, delete, iter));
                workers[i].start();
            }
            
            Timer endTimer = new Timer(false);
            endTimer.schedule(new KillTasks(), (long)(runTimeApprox * 60 * 1000));
            shouldRun = true;
            startTime = System.nanoTime() / 1000000L;
            for (int i = 0; i < numThreads; i++) {
                try {
                    workers[i].join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            long addTime = 0, findTime = 0, deleteTime = 0;
            long totalIters = 0;
            for (int i = 0; i < numThreads; i++) {
                if(DEBUG_TIME) {
                    addTime += add[i];
                    findTime += find[i];
                    deleteTime += delete[i];
                }
                totalIters += iter[i];
            }
            long avgAddTime = 0, avgFindTime = 0, avgDeleteTime = 0;
            if(DEBUG_TIME) {
                avgAddTime = addTime / totalIters;
                avgFindTime = findTime / totalIters;
                avgDeleteTime = deleteTime / totalIters;
            }
            
            double rate_ = 1.0 * totalIters / (endTime - startTime);
            if(numThreads == socketSize) rate0 = rate_;
            double speedup_ = rate_ / rate0;
            double efficiency_ = speedup_ / numThreads;
            
            rate.add(numThreads, rate_);
            speedup.add(numThreads, speedup_);
            efficiency.add(numThreads, efficiency_);
            
            System.out.println("Finished.");
            System.out.println("Threads: " + numThreads);
            if(DEBUG_TIME) {
                System.out.println("Average add rate: " + (1000000000L * numThreads / avgAddTime) + " runs/s");
                System.out.println("Average lookup rate: " + (1000000000L * numThreads / avgFindTime) + " runs/s");
                System.out.println("Average delete rate: " + (1000000000L * numThreads / avgDeleteTime) + " runs/s");
            }
            System.out.printf("Rate: %.4f, Speedup: %.4f, Efficiency: %.4f\n", rate_, speedup_, efficiency_);
            if(DEBUG_TIME) {
                System.out.println("Total run time: " + (addTime + findTime + deleteTime) / 1000000L + " ms");
            }
            System.out.println();
        }
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
        try {
            Runtime.getRuntime().exec("set-cpus all");
        } catch (IOException ex) {}
    }
}
