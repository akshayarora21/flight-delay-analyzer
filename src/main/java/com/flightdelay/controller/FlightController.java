package com.flightdelay.controller;

import com.flightdelay.dto.Responses.*;
import com.flightdelay.service.DelayRiskService;
import com.flightdelay.service.StatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for the Flight Delay Analyzer.
 *
 * Endpoints:
 *   GET /api/v1/delay-risk       — predict delay risk for a route
 *   GET /api/v1/worst-routes     — leaderboard of highest-delay routes
 *   GET /api/v1/airline-stats    — per-airline performance summary
 *   GET /api/v1/worst-airports   — airports with worst departure delays
 *   GET /api/v1/stats            — dataset-level summary
 *   GET /api/v1/health           — liveness check
 */
@RestController
@RequestMapping("/api/v1")
@CrossOrigin  // allows the optional dashboard to call the API
public class FlightController {

    private final DelayRiskService riskService;
    private final StatsService statsService;

    public FlightController(DelayRiskService riskService, StatsService statsService) {
        this.riskService  = riskService;
        this.statsService = statsService;
    }

    /**
     * Predict delay risk for a route.
     *
     * Required: origin, dest
     * Optional: carrier, month (1-12)
     *
     * Example:
     *   GET /api/v1/delay-risk?origin=ORD&dest=LAX&carrier=AA&month=12
     */
    @GetMapping("/delay-risk")
    public ResponseEntity<DelayRiskResponse> getDelayRisk(
        @RequestParam String origin,
        @RequestParam String dest,
        @RequestParam(required = false) String carrier,
        @RequestParam(required = false) Integer month
    ) {
        if (month != null && (month < 1 || month > 12)) {
            return ResponseEntity.badRequest().build();
        }
        DelayRiskResponse response = riskService.predictRisk(origin, dest, carrier, month);
        return ResponseEntity.ok(response);
    }

    /**
     * Top N worst routes by average arrival delay.
     *
     * Optional: limit (default 20), minFlights (default 100)
     *
     * Example:
     *   GET /api/v1/worst-routes?limit=10&minFlights=200
     */
    @GetMapping("/worst-routes")
    public ResponseEntity<List<RouteStats>> getWorstRoutes(
        @RequestParam(defaultValue = "20") int limit,
        @RequestParam(defaultValue = "100") int minFlights
    ) {
        return ResponseEntity.ok(statsService.getWorstRoutes(limit, minFlights));
    }

    /**
     * All airlines ranked by delay performance.
     *
     * Example:
     *   GET /api/v1/airline-stats
     */
    @GetMapping("/airline-stats")
    public ResponseEntity<List<AirlineStats>> getAirlineStats() {
        return ResponseEntity.ok(statsService.getAirlineStats());
    }

    /**
     * Top N airports with worst average departure delays.
     *
     * Optional: limit (default 20), minFlights (default 200)
     *
     * Example:
     *   GET /api/v1/worst-airports?limit=15
     */
    @GetMapping("/worst-airports")
    public ResponseEntity<List<AirportStats>> getWorstAirports(
        @RequestParam(defaultValue = "20") int limit,
        @RequestParam(defaultValue = "200") int minFlights
    ) {
        return ResponseEntity.ok(statsService.getWorstAirports(limit, minFlights));
    }

    /**
     * Dataset-level summary stats.
     *
     * Example:
     *   GET /api/v1/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<DatasetStats> getStats() {
        return ResponseEntity.ok(statsService.getDatasetStats());
    }

    /**
     * Liveness check.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
    
    @GetMapping("/airport/{code}/")
    public ResponseEntity<List<RouteStats>> getAirportRoutes(
    		@PathVariable String code,
    		@RequestParam(defaultValue = "50") int minFlights
    		)
    {
    	return ResponseEntity.ok(statsService.getRoutesByOrigin(code, minFlights));
    }
}
