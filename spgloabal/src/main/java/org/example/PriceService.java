package org.example;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * API definition for producers and consumers for batch processing. Assumption is both the process run on the same JVM.
 */
public interface PriceService {

    record PriceRecord(String id, Instant asOf, Map<String, Object> payload) {}

    /**
     * Starts a new batch.
     * @param batchId unique identifier for the batch
     */
    void startBatch(String batchId);

    /**
     * Uploads a chunk of price records of a batch.
     */
    void uploadChunk(String batchId, List<PriceRecord> records);

    /**
     * Completes a batch and makes all its prices atomically visible.
     */
    void completeBatch(String batchId);

    /**
     * Cancels a batch discards all its data.
     */
    void cancelBatch(String batchId);

    /**
     * Returns the latest price records for the given ids.
     */
    Map<String, PriceRecord> getLastPrices(List<String> ids);
}

