package com.flightdelay.dto;

/**
 * Response DTOs — plain Java records, no framework magic needed.
 */
public class Responses {

    /**
     * Delay risk prediction for a specific route/carrier/month combination.
     */
    public record DelayRiskResponse(
        String origin,
        String dest,
        String carrier,
        Integer month,
        int sampleSize,
        double avgArrivalDelayMinutes,
        double delayRatePercent,       // % of flights arriving >15 min late
        double cancellationRatePercent,
        double riskScore,              // 0-100 composite score
        String riskLabel,              // LOW / MODERATE / HIGH / VERY HIGH
        String insight                 // human-readable summary
    ) {}

    /**
     * A single worst-route entry.
     */
    public record RouteStats(
        String origin,
        String dest,
        long totalFlights,
        double avgDelayMinutes,
        double delayRatePercent
    ) {}

    /**
     * Per-airline performance summary.
     */
    public record AirlineStats(
        String carrier,
        long totalFlights,
        double avgDelayMinutes,
        double delayRatePercent,
        double cancellationRatePercent,
        String performanceLabel        // EXCELLENT / GOOD / AVERAGE / POOR
    ) {}

    /**
     * Per-airport departure performance.
     */
    public record AirportStats(
        String airport,
        long totalFlights,
        double avgDepartureDelayMinutes,
        double departureDelayRatePercent
    ) {}

    /**
     * Summary stats for the /stats endpoint.
     */
    public record DatasetStats(
        long totalFlights,
        long totalRoutes,
        long totalAirlines,
        double overallDelayRatePercent,
        double overallAvgDelayMinutes,
        String dataNote
    ) {}
}
