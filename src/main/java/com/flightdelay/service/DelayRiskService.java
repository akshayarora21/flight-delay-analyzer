package com.flightdelay.service;

import com.flightdelay.dto.Responses.*;
import com.flightdelay.model.Flight;
import com.flightdelay.repository.FlightRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Core business logic for delay risk prediction.
 *
 * Prediction approach: weighted composite of historical signals.
 * No black box — every factor is explicit and explainable.
 *
 *   Risk score (0–100) =
 *     0.40 * delay_rate_score       (% flights arriving >15 min late, scaled 0-100)
 *     0.35 * avg_delay_score        (avg arrival delay minutes, capped at 60 min)
 *     0.25 * cancellation_score     (% cancelled, scaled 0-100)
 *
 * More specific queries (route + carrier + month) take precedence over
 * broader fallbacks (route only, carrier only).
 */
@Service
public class DelayRiskService {

    private static final int MIN_SAMPLE_SIZE = 10;

    private final FlightRepository repo;

    public DelayRiskService(FlightRepository repo) {
        this.repo = repo;
    }

    public DelayRiskResponse predictRisk(String origin, String dest,
                                          String carrier, Integer month) {
        origin  = origin.toUpperCase().trim();
        dest    = dest.toUpperCase().trim();
        carrier = carrier != null ? carrier.toUpperCase().trim() : null;

        List<Flight> flights = fetchFlights(origin, dest, carrier, month);

        if (flights.size() < MIN_SAMPLE_SIZE) {
            return insufficientDataResponse(origin, dest, carrier, month, flights.size());
        }

        // Core stats
        long nonCancelled = flights.stream().filter(f -> !f.isCancelled()).count();
        long cancelled    = flights.size() - nonCancelled;

        double avgArrDelay = flights.stream()
            .filter(f -> !f.isCancelled() && f.getArrivalDelay() != null)
            .mapToDouble(Flight::getArrivalDelay)
            .average()
            .orElse(0.0);

        long delayedCount = flights.stream()
            .filter(f -> !f.isCancelled()
                      && f.getArrivalDelay() != null
                      && f.getArrivalDelay() > 15)
            .count();

        double delayRate  = nonCancelled > 0 ? (double) delayedCount / nonCancelled * 100 : 0;
        double cancelRate = (double) cancelled / flights.size() * 100;

        double riskScore = computeRiskScore(delayRate, avgArrDelay, cancelRate);
        String riskLabel = riskLabel(riskScore);
        String insight   = buildInsight(origin, dest, carrier, month,
                                        delayRate, avgArrDelay, cancelRate,
                                        flights.size(), riskLabel);

        return new DelayRiskResponse(
            origin, dest,
            carrier != null ? carrier : "ALL",
            month,
            flights.size(),
            round(avgArrDelay),
            round(delayRate),
            round(cancelRate),
            round(riskScore),
            riskLabel,
            insight
        );
    }

    // --- Private helpers ---

    private List<Flight> fetchFlights(String origin, String dest,
                                       String carrier, Integer month) {
        // Most specific first — fall back progressively
        if (carrier != null && month != null) {
            List<Flight> f = repo.findByRouteCarrierAndMonth(origin, dest, carrier, month);
            if (f.size() >= MIN_SAMPLE_SIZE) return f;
        }
        if (carrier != null) {
            List<Flight> f = repo.findByRouteAndCarrier(origin, dest, carrier);
            if (f.size() >= MIN_SAMPLE_SIZE) return f;
        }
        return repo.findByRoute(origin, dest);
    }

    /**
     * Weighted composite risk score.
     *   delay_rate_score  : delay rate (0-100%) mapped linearly
     *   avg_delay_score   : avg delay capped at 60 min → 0-100
     *   cancel_score      : cancel rate (0-100%) mapped linearly (0-20% range → 0-100)
     */
    private double computeRiskScore(double delayRate, double avgDelay, double cancelRate) {
        double delayRateScore  = Math.min(delayRate, 100.0);
        double avgDelayScore   = Math.min(avgDelay / 60.0 * 100.0, 100.0);
        double cancelScore     = Math.min(cancelRate / 20.0 * 100.0, 100.0);

        return 0.40 * delayRateScore
             + 0.35 * avgDelayScore
             + 0.25 * cancelScore;
    }

    private String riskLabel(double score) {
        if (score < 25)  return "LOW";
        if (score < 50)  return "MODERATE";
        if (score < 75)  return "HIGH";
        return "VERY HIGH";
    }

    private String buildInsight(String origin, String dest, String carrier,
                                 Integer month, double delayRate,
                                 double avgDelay, double cancelRate,
                                 int sample, String label) {
        String carrierPart = carrier != null ? " on " + carrier : "";
        String monthPart   = month   != null ? " in month " + month : "";

        return String.format(
            "%s→%s%s%s: %s risk. " +
            "%.1f%% of flights arrive >15 min late, " +
            "avg delay %.1f min, " +
            "%.1f%% cancellation rate. " +
            "Based on %d historical flights.",
            origin, dest, carrierPart, monthPart, label,
            delayRate, avgDelay, cancelRate, sample
        );
    }

    private DelayRiskResponse insufficientDataResponse(String origin, String dest,
                                                        String carrier, Integer month,
                                                        int size) {
        return new DelayRiskResponse(
            origin, dest,
            carrier != null ? carrier : "ALL",
            month,
            size,
            0, 0, 0, -1,
            "UNKNOWN",
            "Insufficient data for " + origin + "→" + dest +
            " (only " + size + " records found; need at least " + MIN_SAMPLE_SIZE + ")."
        );
    }

    private double round(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
