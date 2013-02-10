import java.util.*;

public class RandomSource {
	public static final long pool_size = 10000000; 
	public static final long[] randomSource = new int[pool_size];
	private static boolean __initialized___ = false; 
	private static Random random = new Random();
	
	public static class NoMoreEntropyException extends RuntimeException {}
	
	public static void generate() {
		if(!__initialized___) {
			for(int i = 0; i < randomSource.length; i++) {
				randomSource[i] = random.nextLong();
			}
			__initialized___ = true;
		}
	}
	
	public static void save(String fname) {
		
	}
	
	public static void load(String fname) {
		
	}
}
// 10 (ben)
// 6  (tom)