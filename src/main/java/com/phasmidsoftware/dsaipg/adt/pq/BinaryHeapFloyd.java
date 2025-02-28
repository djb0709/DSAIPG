package com.phasmidsoftware.dsaipg.adt.pq;

import com.phasmidsoftware.dsaipg.util.Benchmark_Timer;

import java.util.Comparator;
import java.util.Random;
import java.util.function.Supplier;

public class BinaryHeapFloyd {
    static final int M = 3276000;
    static final int insertCount = 12800000;
    static final int removeCount = 3200000;
    //max heap no floyd's
    public static void main(String[] args) throws PQException {


        Supplier<PriorityQueue<Integer>> supplier = () -> {
            return new PriorityQueue<>(M, true, Comparator.comparing(Integer::intValue), true);
        };

        Benchmark_Timer<PriorityQueue<Integer>> benchmark = new Benchmark_Timer<>(
                "PQ Basic Binary Heap + Floyd",
                pqInstance -> {
                    // -- insert --
                    Random random = new Random();
                    long startInsertion = System.nanoTime();
                    for(int i = 0; i < insertCount; i++){
                        pqInstance.give(random.nextInt());
                    }
                    long endInsertion = System.nanoTime();
                    double insertionTimeMs = (endInsertion - startInsertion) / 1e6;

                    // remove--
                    long startRemoval = System.nanoTime();
                    for(int i = 0; i < removeCount; i++){
                        try {
                            pqInstance.take();
                        } catch (PQException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    long endRemoval = System.nanoTime();
                    double removalTimeMs = (endRemoval - startRemoval) / 1e6;

                    System.out.printf("[Run Detail] Insertion: %.3f ms, Removal: %.3f ms\n",
                            insertionTimeMs, removalTimeMs);
                },
                pqInstance -> {
                    var spilled = pqInstance.getSpilledList();
                    if(!spilled.isEmpty()){
                        Integer maxSpilled = spilled.stream().max(Comparator.naturalOrder()).orElse(null);
                        System.out.println("Max in spilled list: " + maxSpilled);
                    }
                }
        );

        // 5 runs
        int m = 5;
        double time = benchmark.runFromSupplier(supplier, m);

        //
        System.out.printf("BinaryHeapFloyd (M=%d, inserts=%d, removes=%d), average total time: %.5f ms%n",
                M, insertCount, removeCount, time);
    }



}
