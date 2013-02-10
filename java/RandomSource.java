import java.util.*;
import java.io.*;

public class RandomSource {
	public static int pool_size = 1000000; 
	public static final long[] randomSource = new long[pool_size];
	private static boolean __initialized___ = false; 
	private static Random random = new Random();

	public static class NoMoreEntropyException extends RuntimeException {}
	
	public static void generate() {
		if(!__initialized___) {
			for(int i = 0; i < randomSource.length; i++) {
				while(randomSource[i] <= 0)
					randomSource[i] = random.nextLong();
			}
			__initialized___ = true;
		}
	}
	
	public static void save(String fname) {
		try {
		FileWriter storageFile = new FileWriter(fname);
		BufferedWriter os = new BufferedWriter(storageFile);
		for(int i = 0; i < randomSource.length; i++) {
			os.write("" + randomSource[i]);
			if(i < randomSource.length - 1) os.write("\n");
		}
		os.flush();
		os.close();
		} catch(Exception e) {
			System.out.println("Input error");
			System.exit(0);
		}
	}
	
	public static void load(String fname) {
		try {
			FileInputStream storageFile = new FileInputStream(fname);
			long number = 0L;
			int index = 0;
			int character = 0;
			while((character = storageFile.read()) != -1) {
				if(index >= pool_size) break;
				if('0' <= character && character <= '9')
					number = number * 10 + character - '0';
				else if(character == '\n' || character == '\r') {
					randomSource[index] = number;
					number = 0L;
					index++;
				} else {
					throw new IOException("Invalid input");
				}
			}
		} catch(IOException ex) {
			System.out.println("Input error");
			System.out.println("Generating new random");
			generate();
			System.out.println("Saving");
			save(fname);
		}
	}
	
	public static void main(String[] args) {
		generate();
		save("numbers");
	}
}
// 10 (ben)
// 6  (tom)
