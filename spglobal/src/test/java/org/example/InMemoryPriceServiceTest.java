package org.example;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryPriceServiceTest {

    Logger logger = Logger.getLogger(InMemoryPriceServiceTest.class.getName());

    private final InMemoryPriceService service =
            new InMemoryPriceService(8);

    @AfterEach
    void tearDown() {
        service.close();
    }

    // Visibility test to make sure, data is not visible until the testing is complete
    @Test
    void dataNotVisibleBeforeCompletion() {

        service.startBatch("b1");
        service.uploadChunk("b1", List.of(
                new PriceService.PriceRecord(
                        "A",
                        Instant.now(),
                        Map.of("price", 100))
        ));
        // This assertion will ensure the record is not visible until the batch is processed
        assertTrue(service.getLastPrices(List.of("A")).isEmpty());
        service.completeBatch("b1");
        Map<String, PriceService.PriceRecord> result = service.getLastPrices(List.of("A"));
        assertEquals(100, result.get("A").payload().get("price"));
    }

    // Test to make sure latest value is determined by asOf
    @Test
    void lastValueWinsByAsOf() {

        Instant t1 = Instant.parse("2026-01-01T00:00:00Z");
        Instant t2 = Instant.parse("2026-01-02T00:00:00Z");
        service.startBatch("b1");
        service.uploadChunk("b1", List.of(new PriceService.PriceRecord("A", t1, Map.of("price", 100))
        ));
        service.completeBatch("b1");

        service.startBatch("b2");
        service.uploadChunk("b2", List.of(new PriceService.PriceRecord("A", t2, Map.of("price", 200))
        ));
        service.completeBatch("b2");

        Map<String, PriceService.PriceRecord> result = service.getLastPrices(List.of("A"));

        assertEquals(200, result.get("A").payload().get("price"));
    }

    // Test to ensure producer is able to cancel batch
    @Test
    void cancelledBatchIsDiscarded() {

        service.startBatch("b1");
        service.uploadChunk("b1", List.of(
                new PriceService.PriceRecord(
                        "A",
                        Instant.now(),
                        Map.of("price", 100))
        ));
        service.cancelBatch("b1");

        assertTrue(service.getLastPrices(List.of("A")).isEmpty());
    }

    // Parallel processing to uploads the chunks in same batch
    @Test
    void parallelChunkUploads() throws Exception {
        logger.log(Level.INFO, "Check upload test");

        service.startBatch("b1");
        ExecutorService executor = Executors.newFixedThreadPool(4);
        List<Callable<Void>> tasks = IntStream.range(0, 10000)
                .mapToObj(i -> (Callable<Void>) () -> {
                    service.uploadChunk("b1", List.of(
                            new PriceService.PriceRecord(
                                    "ID-" + i,
                                    Instant.now(),
                                    Map.of("price", i))
                    ));
                    return null;
                }).toList();

        executor.invokeAll(tasks);
        executor.shutdown();

        service.completeBatch("b1");

        Map<String, PriceService.PriceRecord> result =
                service.getLastPrices(
                        IntStream.range(0, 100)
                                .mapToObj(i -> "ID-" + i)
                                .toList());

        assertEquals(100, result.size());
    }

    // --------------------------------------------------------
    // Parallell processing of multiple batchs - Assume there are more than one producer are publishing the data
    // --------------------------------------------------------
    @Test
    void concurrentBatchCompletion() throws Exception {

        int batchCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(batchCount);

        for (int i = 0; i < batchCount; i++) {
            String batchId = "b" + i;
            service.startBatch(batchId);

            service.uploadChunk(batchId, List.of(
                    new PriceService.PriceRecord(
                            "ID-" + i,
                            Instant.now(),
                            Map.of("price", i))
            ));
        }

        List<Callable<Void>> tasks = IntStream.range(0, batchCount)
                .mapToObj(i -> (Callable<Void>) () -> {
                    service.completeBatch("b" + i);
                    return null;
                }).toList();

        executor.invokeAll(tasks);
        executor.shutdown();

        Map<String, PriceService.PriceRecord> result =
                service.getLastPrices(
                        IntStream.range(0, batchCount)
                                .mapToObj(i -> "ID-" + i)
                                .toList());

        assertEquals(batchCount, result.size());
    }

    // --------------------------------------------------------
    // Ensure records are not visible until the processing is completed.
    // --------------------------------------------------------
    @Test
    void noPartialVisibilityDuringProcessing() throws Exception {

        service.startBatch("b1");

        for (int i = 0; i < 1000; i++) {
            service.uploadChunk("b1", List.of(
                    new PriceService.PriceRecord(
                            "ID-" + i,
                            Instant.now(),
                            Map.of("price", i))
            ));
        }

        // Call complete in another thread
        CompletableFuture<Void> future =
                CompletableFuture.runAsync(() ->
                        service.completeBatch("b1"));

        // While completing, repeatedly check visibility
        while (!future.isDone()) {
            Map<String, PriceService.PriceRecord> snapshot =
                    service.getLastPrices(List.of("ID-1"));

            // Should either be empty or fully completed
            if (!snapshot.isEmpty()) {
                assertEquals(1000,
                        service.getLastPrices(
                                IntStream.range(0, 1000)
                                        .mapToObj(i -> "ID-" + i)
                                        .toList()).size());
                break;
            }
        }

        future.join();
    }
}