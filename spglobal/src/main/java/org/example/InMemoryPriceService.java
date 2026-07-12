package org.example;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class InMemoryPriceService implements PriceService, AutoCloseable {

    Logger logger = Logger.getLogger( InMemoryPriceService.class.getName());

/**
*  Inner class to create the concurrent hashmap and completable future tasks to work with multiple threads.
*/
    private static class BatchContext {
        final ConcurrentMap<String, PriceRecord> updates = new ConcurrentHashMap<>();
        final List<CompletableFuture<?>> tasks = new CopyOnWriteArrayList<>();
        volatile boolean completing = false;
    }

    private final ConcurrentMap<String, BatchContext> batches = new ConcurrentHashMap<>();

    private final AtomicReference<Map<String, PriceRecord>> currentPrices =
            new AtomicReference<>(Map.of());

    private final ExecutorService executor;

    // Constructor create the fixed no of threads.
    public InMemoryPriceService(int workerThreads) {
        this.executor = Executors.newFixedThreadPool(workerThreads);
    }

    // ---------- Producer API ----------

    @Override
    public void startBatch(String batchId) {
        validateBatchId(batchId);

        if (batches.putIfAbsent(batchId, new BatchContext()) != null) {
            logger.log(Level.SEVERE, "Batch already exists ", batchId);
            throw new IllegalStateException("Batch already exists: " + batchId);
        }
        logger.info("Entered into processOrder method for ID: {}");
        logger.info(" after Entered into processOrder method for ID: {}");
        logger.info(" third Entered into processOrder method for ID: {}");
        logger.info(" fourth Entered into processOrder method for ID: {}");



        logger.log(Level.INFO, "Batch {0} is starting ", batchId);
    }

    @Override
    public void uploadChunk(String batchId, List<PriceRecord> records) {
        BatchContext batch = getActiveBatch(batchId);

        if (batch.completing) {
            logger.log(Level.SEVERE, "Batch already completing ", batchId);
            throw new IllegalStateException("Batch already completing: " + batchId);
        }

        CompletableFuture<?> task = CompletableFuture.runAsync(() -> {
            for (PriceRecord record : records) {
                validateRecord(record);
                // this is important to store the latest values based on the time
                batch.updates.merge(
                        record.id(),
                        record,
                        (existing, incoming) ->
                                incoming.asOf().isAfter(existing.asOf()) ? incoming : existing
                );
            }
        }, executor);
        logger.log(Level.SEVERE, "Batch-task {0} starting ", batchId + task.toString());
        batch.tasks.add(task);
    }

    @Override
    public void completeBatch(String batchId) {
        logger.log(Level.INFO, "Completing Batch {0} ", batchId );
        BatchContext batch = batches.get(batchId);
        if (batch == null) {
            throw new IllegalStateException("Batch not found: " + batchId);
        }

        batch.completing = true;

        // Wait for all parallel chunk-processing tasks to finish
        CompletableFuture
                .allOf(batch.tasks.toArray(new CompletableFuture[0]))
                .join();

        // Perform atomic snapshot merge asynchronously
        CompletableFuture.runAsync(() -> {
            mergeIntoSnapshot(batch.updates);
            batches.remove(batchId);
        }, executor).join();
    }

    @Override
    public void cancelBatch(String batchId) {
        logger.log(Level.SEVERE, "Cancelling Batch {0} ", batchId );
        BatchContext batch = batches.remove(batchId);
        logger.log(Level.SEVERE, "Batch {0} removed", batchId );
        if (batch == null) {
            throw new IllegalStateException("Batch not found: " + batchId);
        }
    }

    // ---------- Consumer API ----------

    @Override
    public Map<String, PriceRecord> getLastPrices(List<String> ids) {
        Map<String, PriceRecord> snapshot = currentPrices.get();
        Map<String, PriceRecord> result = new HashMap<>();

        for (String id : ids) {
            PriceRecord record = snapshot.get(id);
            if (record != null) {
                result.put(id, record);
            }
        }

        return result;
    }

    // ---------- Internal Logic ----------

    private void mergeIntoSnapshot(Map<String, PriceRecord> updates) {
        while (true) {
            Map<String, PriceRecord> current = currentPrices.get();
            Map<String, PriceRecord> newSnapshot = new HashMap<>(current);

            for (PriceRecord record : updates.values()) {
                newSnapshot.merge(
                        record.id(),
                        record,
                        (existing, incoming) ->
                                incoming.asOf().isAfter(existing.asOf()) ? incoming : existing
                );
            }

            Map<String, PriceRecord> unmodifiable =
                    Collections.unmodifiableMap(newSnapshot);

            if (currentPrices.compareAndSet(current, unmodifiable)) {
                return;
            }
        }
    }

    private BatchContext getActiveBatch(String batchId) {
        BatchContext batch = batches.get(batchId);
        if (batch == null) {
            logger.log(Level.SEVERE, "Batch {0} not started ", batchId);
            throw new IllegalStateException("Batch not started: " + batchId);
        }
        logger.log(Level.INFO, "Batch id {0} started ", batchId);
        return batch;
    }

    private void validateBatchId(String batchId) {
        if (batchId == null || batchId.isBlank()) {
            logger.log(Level.SEVERE, "Batch id {0} invalid ", batchId);
            throw new IllegalArgumentException("Batch id invalid");
        }
    }

    private void validateRecord(PriceRecord record) {
        if (record == null ||
                record.id() == null ||
                record.asOf() == null ||
                record.payload() == null) {
            logger.log(Level.SEVERE, "Invalid record {0}", record.toString());
            throw new IllegalArgumentException("Invalid record");
        }
    }

    @Override
    public void close() {
        executor.shutdown();
    }
}
