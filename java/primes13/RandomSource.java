package primes13;

import java.util.*;
import java.io.*;

public class RandomSource {
    private long x;
    private long a = 1103515245L;
    private long b = 12345L;
    private long m = 1L << 31;
    private long shift = 0L;
    
    public RandomSource(int seed) {
        x = seed >>> 1;
    }
    
    public long next() {
        x = (x * a + b) % m;
        return x >> shift;
    }
}
