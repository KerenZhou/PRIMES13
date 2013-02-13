package primes13;

public class LazySkipListTester {
	final static int[] TOM = { 6, 48 };
	final static int[] BEN = { 10, 80 };
	final static int maxThreads = Runtime.getRuntime().availableProcessors();
	final static int socketSize;
	static int numThreads = 0;
	static int runTimes;
	private static LazySkipList skipList;
	private static long startTime;
	private static long endTime;
	
	static {
		int size;
		if(maxThreads == TOM[1]) {
			size = TOM[0];
		} else if(maxThreads == BEN[1]) {
			size = BEN[0];
		} else {
			size = 1;
		}
		socketSize = size;
	}
	
	private static class TestThread implements Runnable {
		int id;
                long[] add, find, delete;
                
		public TestThread(int id, long[] add, long[] find) {
			this.id = id;
                        this.add = add;
                        this.find = find;
		}
		
		private void add(int i) {
			skipList.add(RandomSource.randomSource[id*runTimes+i]);
		}
		
		private void find(int i) {
			skipList.contains(RandomSource.randomSource[id*runTimes+i]);
		}
                
		private void remove(int i) {
			skipList.remove(RandomSource.randomSource[id*runTimes+i]);
		}
		
		public void run() {
                        while(System.nanoTime() - startTime < 0) ;
                        long nanoTime, total;
                        
			for(int i = 0; i < runTimes; i++) {
                                nanoTime = System.nanoTime();
				add(i);
                                total = System.nanoTime() - nanoTime;
                                add[id] += total;
			}
			for(int i = 0; i < runTimes; i++) {
                                nanoTime = System.nanoTime();
				find(i);
                                total = System.nanoTime() - nanoTime;
                                find[id] += total;
			}
			/*for(int i = 0; i < runTimes / 10; i++) {
				int d = RandomSource.random.nextInt(runTimes);
				remove(d);
			}*/
			if(id + 1 == numThreads) endTime = System.nanoTime();
		}
	}
	
	public static void main(String[] args) {
		RandomSource.load("numbers");
                System.out.print("\n\n");
		while(numThreads < maxThreads) {
			numThreads += socketSize;
			runTimes = RandomSource.pool_size / numThreads;
			System.out.println("Starting with: " + numThreads + " threads.");
			Thread[] workers = new Thread[numThreads];
			startTime = System.nanoTime() + 1000000L;
			skipList = new LazySkipList();
                        long[] add = new long[numThreads], find = new long[numThreads];
			for(int i = 0; i < numThreads; i++) {
				workers[i] = new Thread(new TestThread(i, add, find));
				workers[i].start();
			}
			for(int i = 0; i < numThreads; i++) {
				try {
					workers[i].join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
                        long addTime = 0, findTime = 0;
                        for(int i = 0; i < numThreads; i++) {
                            addTime += add[i];
                            findTime += find[i];
                        }
                        long avgAddTime = addTime / RandomSource.pool_size;
                        long avgFindTime = findTime / RandomSource.pool_size;
                        System.out.println("Finished.");
			System.out.println("Threads: " + numThreads);
                        System.out.println("Average add time: " + avgAddTime + "ns");
                        System.out.println("Average lookup time: " + avgFindTime + "ns");
                        System.out.println("Run time: " + (endTime - startTime) + "ns");
                        System.out.println("Total run time: " + (addTime + findTime) + "ns");
                        System.out.println("Per-thread stats: ");
                        for(int i = 0; i < numThreads; i++) {
                            System.out.print("Thread " + i + " - ");
                            System.out.print("Average add time: " + (add[i] / runTimes) + "ns, ");
                            System.out.print("Average find time: " + (find[i] / runTimes) + "ns, ");
                            System.out.println("Total run time: " + (add[i] + find[i]) + "ns");
                        }
                        System.out.println("\n");
		}
	}
}
