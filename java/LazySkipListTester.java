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
		public TestThread(int id) {
			this.id = id;
		}
		public void run() {
			while(System.nanoTime() - startTime < 0) ;
			for(int i = 0; i < runTimes; i++) {
				skipList.add(RandomSource.randomSource[id*runTimes+i]);
			}
			if(id + 1 == numThreads) endTime = System.nanoTime();
		}
	}
	
	public static void main(String[] args) {
		RandomSource.load("numbers");
		while(numThreads < maxThreads) {
			numThreads += socketSize;
			runTimes = RandomSource.pool_size / numThreads;
			System.out.print(numThreads + " ");
			Thread[] workers = new Thread[numThreads];
			startTime = System.nanoTime() + 1000000L;
			skipList = new LazySkipList();
			for(int i = 0; i < numThreads; i++) {
				workers[i] = new Thread(new TestThread(i));
				workers[i].start();
			}
			for(int i = 0; i < numThreads; i++) {
				try {
					workers[i].join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			System.out.println((endTime - startTime) + "ns");
		}
	}
}
