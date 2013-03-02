/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primes13;

/**
 *
 * @author pat
 */
public class ModTester {
    public static void main(String[] args) {
        long a = 1 << 31;
        long dummy;
        long b = 0xDEADBEEF;
        long iterl = 1000000000L;
        long sum = 0;
        long start = System.nanoTime();
        for(int i = 0; i < iterl; i++) {
            dummy = b % a;
            sum += dummy;
            b++;
        }
        long num1 = iterl * 1000000 / (System.nanoTime() - start);
        System.out.println(sum);
        System.out.println(iterl * 1000000 / (System.nanoTime() - start));
        sum = 0;
        start = System.nanoTime();
        for(int i = 0; i < iterl; i++) {
            dummy = b & (a - 1);
            sum += dummy;
            b++;
        }
        long num2 = iterl * 1000000 / (System.nanoTime() - start);
        System.out.println(sum);
        System.out.println(iterl * 1000000 / (System.nanoTime() - start));
        System.out.println(num2 - num1);
    }
}
