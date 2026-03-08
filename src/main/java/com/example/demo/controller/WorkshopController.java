package com.example.demo.controller;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * REST Controller for the metrics workshop.
 * Demonstrates various Micrometer metrics: counters, gauges, timers/histograms.
 */
@RestController
public class WorkshopController {

    private static final Logger log = LoggerFactory.getLogger(WorkshopController.class);
    private static final Random random = new Random();

    private final MeterRegistry meterRegistry;

    // Gauges - thread-safe using AtomicInteger
    private final AtomicInteger queueSize = new AtomicInteger(0);
    private final AtomicInteger activeRequests = new AtomicInteger(0);

    // Global mode flags - volatile for visibility across threads
    private volatile boolean slowModeEnabled = false;
    private volatile boolean errorModeEnabled = false;

    public WorkshopController(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Register gauges - these track the current value of the AtomicIntegers
        meterRegistry.gauge("workshop_queue_size", queueSize);
        meterRegistry.gauge("workshop_active_requests", activeRequests);

        log.info("WorkshopController initialized with custom metrics");
    }

    /**
     * Fast endpoint - returns immediately.
     */
    @GetMapping("/fast")
    public ResponseEntity<String> fast() {
        return handleRequest("fast", () -> {
            // Fast endpoint - no artificial delay
            log.debug("Fast endpoint called");
        });
    }

    /**
     * Slow endpoint - sleeps for 300-500ms before returning.
     */
    @GetMapping("/slow")
    public ResponseEntity<String> slow() {
        return handleRequest("slow", () -> {
            int delay = 300 + random.nextInt(201); // 300-500ms
            log.debug("Slow endpoint called, sleeping for {}ms", delay);
            sleep(delay);
        });
    }

    /**
     * Error endpoint - returns HTTP 500 randomly (~30% of requests).
     */
    @GetMapping("/error")
    public ResponseEntity<String> error() {
        return handleRequest("error", () -> {
            log.debug("Error endpoint called");
            // This endpoint has a base ~30% error rate
        }, 0.30);
    }

    /**
     * Enable global slow mode - all requests will be delayed.
     */
    @PostMapping("/enable-slow")
    public ResponseEntity<String> enableSlowMode() {
        slowModeEnabled = true;
        log.info("Slow mode ENABLED - all requests will be delayed");
        incrementCounter("enable-slow", "200");
        return ResponseEntity.ok("Slow mode enabled");
    }

    /**
     * Enable global error mode - higher error rate for all endpoints.
     */
    @PostMapping("/enable-errors")
    public ResponseEntity<String> enableErrorMode() {
        errorModeEnabled = true;
        log.info("Error mode ENABLED - higher error rate active");
        incrementCounter("enable-errors", "200");
        return ResponseEntity.ok("Error mode enabled");
    }

    /**
     * Disable all artificial modes.
     */
    @PostMapping("/disable-modes")
    public ResponseEntity<String> disableModes() {
        slowModeEnabled = false;
        errorModeEnabled = false;
        log.info("All artificial modes DISABLED");
        incrementCounter("disable-modes", "200");
        return ResponseEntity.ok("All modes disabled");
    }

    /**
     * Handles a request with metrics tracking.
     * Default error rate is 0% (no random errors).
     */
    private ResponseEntity<String> handleRequest(String endpoint, Runnable work) {
        return handleRequest(endpoint, work, 0.0);
    }

    /**
     * Handles a request with metrics tracking and configurable error rate.
     */
    private ResponseEntity<String> handleRequest(String endpoint, Runnable work, double baseErrorRate) {
        // Track active requests and queue size
        activeRequests.incrementAndGet();
        queueSize.incrementAndGet();

        Timer.Sample timerSample = Timer.start(meterRegistry);

        try {
            // Apply global slow mode if enabled
            if (slowModeEnabled) {
                int extraDelay = 200 + random.nextInt(301); // 200-500ms extra
                log.debug("Slow mode active, adding {}ms delay", extraDelay);
                sleep(extraDelay);
            }

            // Execute the endpoint-specific work
            work.run();

            // Determine if this request should error
            double effectiveErrorRate = baseErrorRate;
            if (errorModeEnabled) {
                effectiveErrorRate = Math.min(0.70, baseErrorRate + 0.40); // Add 40%, cap at 70%
            }

            if (random.nextDouble() < effectiveErrorRate) {
                // Simulate error
                log.warn("Simulated error on endpoint /{}", endpoint);
                incrementCounter(endpoint, "500");
                incrementErrorCounter();
                recordTimer(timerSample, endpoint, "500");
                return ResponseEntity.internalServerError().body("Simulated error");
            }

            // Success
            log.debug("Request to /{} completed successfully", endpoint);
            incrementCounter(endpoint, "200");
            recordTimer(timerSample, endpoint, "200");
            return ResponseEntity.ok("OK from /" + endpoint);

        } catch (Exception e) {
            log.error("Unexpected error on endpoint /{}: {}", endpoint, e.getMessage());
            incrementCounter(endpoint, "500");
            incrementErrorCounter();
            recordTimer(timerSample, endpoint, "500");
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());

        } finally {
            // Always decrement counters
            activeRequests.decrementAndGet();
            queueSize.decrementAndGet();
        }
    }

    /**
     * Increment the request counter with endpoint and status tags.
     */
    private void incrementCounter(String endpoint, String status) {
        Counter.builder("workshop_requests_total")
                .description("Total number of workshop requests")
                .tag("endpoint", endpoint)
                .tag("status", status)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Increment the error counter for 5xx responses.
     */
    private void incrementErrorCounter() {
        Counter.builder("workshop_errors_total")
                .description("Total number of 5xx errors")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record request duration using Timer.
     */
    private void recordTimer(Timer.Sample sample, String endpoint, String status) {
        sample.stop(Timer.builder("workshop_request_duration")
                .description("Request duration in seconds")
                .tag("endpoint", endpoint)
                .tag("status", status)
                .publishPercentileHistogram()
                .register(meterRegistry));
    }

    /**
     * Sleep helper that handles InterruptedException.
     */
    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Sleep interrupted");
        }
    }
}

