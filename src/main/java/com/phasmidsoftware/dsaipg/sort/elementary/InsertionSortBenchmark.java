package com.phasmidsoftware.dsaipg.sort.elementary;

import com.phasmidsoftware.dsaipg.util.Benchmark_Timer;
import com.phasmidsoftware.dsaipg.util.Config;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.function.Supplier;


public class InsertionSortBenchmark {
    public static void main(String[] args) throws IOException {

        int[] sizes = {100, 500, 1000, 5000, 10000};
        Random random = new Random();

        System.out.println("Benchmarking Insertion Sort on Different Array Orders:\n");

        for (int n : sizes) {
            Integer[] randomArray = generateRandomArray(n, random);
            Integer[] orderedArray = generateOrderedArray(n);
            Integer[] partiallyOrderedArray = generatePartiallyOrderedArray(n, random);
            Integer[] reverseOrderedArray = generateReverseOrderedArray(n);

            System.out.println("Array Size: " + n);

            runBenchmark("Random", () -> Arrays.copyOf(randomArray, n), n);
            runBenchmark("Ordered", () -> Arrays.copyOf(orderedArray, n), n);
            runBenchmark("Partially Ordered", () -> Arrays.copyOf(partiallyOrderedArray, n), n);
            runBenchmark("Reverse Ordered", () -> Arrays.copyOf(reverseOrderedArray, n), n);

            System.out.println();
        }
    }
    private static void runBenchmark(String type, Supplier<Integer[]> arraySupplier, int n) throws IOException {
        // create InsertionSortComparator
        InsertionSortComparator<Integer> sorter = new InsertionSortComparator<>("Insertion Sort", Integer::compareTo, n, 1, Config.load(InsertionSortComparator.class));

        // timer
        Benchmark_Timer<Integer[]> benchmark = new Benchmark_Timer<>("InsertionSort - " + type,
                xs -> sorter.sort(xs, 0, xs.length));


        double time = benchmark.runFromSupplier(arraySupplier, 5); // run 5 times
        System.out.printf("  %-20s: %.3f ms%n", type, time);
    }
    // generate random array
    private static Integer[] generateRandomArray(int n, Random random) {
        Integer[] array = new Integer[n];
        for (int i = 0; i < n; i++) {
            array[i] = random.nextInt(n);
        }
        return array;
    }
    //generate ordered array
    private static Integer[] generateOrderedArray(int n) {
        Integer[] array = new Integer[n];
        for (int i = 0; i < n; i++) {
            array[i] = i;
        }
        return array;
    }

    private static void swap(Integer[] array, int i, int j) {
        Integer temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }
    // partially-ordered
    private static Integer[] generatePartiallyOrderedArray(int n, Random random) {
        Integer[] array = generateOrderedArray(n);
        for (int i = 0; i < n / 10; i++) {
            int idx1 = random.nextInt(n);
            int idx2 = random.nextInt(n);
            swap(array, idx1, idx2);
        }
        return array;
    }
    //reverse ordered
    private static Integer[] generateReverseOrderedArray(int n) {
        Integer[] array = new Integer[n];
        for (int i = 0; i < n; i++) {
            array[i] = n - i;
        }
        return array;
    }
}
