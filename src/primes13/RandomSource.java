package primes13;

import java.util.*;
import java.io.*;

public class RandomSource {
    private long x;
    public static final long a = 1103515245L;
    public static final long b = 12345L;
    public static final long m = 1L << 31;
    
    public RandomSource(long seed) {
        x = seed;
    }

    public RandomSource() {
        this(System.nanoTime());
    }
    
    public long next() {
        x = (x * a + b) & (m - 1);
        return x;
    }

    public double nextDouble() {
        return next() * 1.0 / m;
    }
}
