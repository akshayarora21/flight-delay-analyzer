package com.flightdelay.service;

import com.flightdelay.dto.Responses.*;
import com.flightdelay.repository.FlightRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StatsService {

    private final FlightRepository repo;

    public StatsService(FlightRepository repo) {
        this.repo = repo;
    }

    public List<RouteStats> getWorstRoutes(int limit, int minFlights) {
        return repo.findWorstRoutes(minFlights).stream()
            .limit(limit)
            .map(row -> new RouteStats(
                (String) row[0],                        // origin
                (String) row[1],                        // dest
                ((Number) row[2]).longValue(),           // totalFlights
                round(((Number) row[3]).doubleValue()),  // avgDelay
                round(((Number) row[4]).doubleValue() * 100) // delayRate %
            ))
            .toList();
    }

    public List<AirlineStats> getAirlineStats() {
        return repo.findAirlineStats().stream()
            .map(row -> {
                double avgDelay  = ((Number) row[2]).doubleValue();
                double delayRate = ((Number) row[3]).doubleValue() * 100;
                return new AirlineStats(
                    (String) row[0],
                    ((Number) row[1]).longValue(),
                    round(avgDelay),
                    round(delayRate),
                    round(((Number) row[4]).doubleValue() * 100),
                    performanceLabel(avgDelay, delayRate)
                );
            })
            .toList();
    }

    public List<AirportStats> getWorstAirports(int limit, int minFlights) {
        return repo.findWorstAirports(minFlights).stream()
            .limit(limit)
            .map(row -> new AirportStats(
                (String) row[0],
                ((Number) row[1]).longValue(),
                round(((Number) row[2]).doubleValue()),
                round(((Number) row[3]).doubleValue() * 100)
            ))
            .toList();
    }

    public DatasetStats getDatasetStats() {
        long total = repo.count();

        List<Object[]> airlineRows = repo.findAirlineStats();
        long airlines = airlineRows.size();

        List<Object[]> routeRows = repo.findWorstRoutes(1);
        long routes = routeRows.size();

        double overallDelay = airlineRows.stream()
            .mapToDouble(r -> ((Number) r[3]).doubleValue())
            .average().orElse(0) * 100;

        double overallAvg = airlineRows.stream()
            .mapToDouble(r -> ((Number) r[2]).doubleValue())
            .average().orElse(0);

        return new DatasetStats(
            total,
            routes,
            airlines,
            round(overallDelay),
            round(overallAvg),
            "Data sourced from BTS On-Time Performance dataset. " +
            "A flight is 'delayed' if it arrives more than 15 minutes late."
        );
    }

    private String performanceLabel(double avgDelay, double delayRate) {
        if (avgDelay < 5  && delayRate < 15) return "EXCELLENT";
        if (avgDelay < 10 && delayRate < 25) return "GOOD";
        if (avgDelay < 20 && delayRate < 35) return "AVERAGE";
        return "POOR";
    }

    private double round(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
    
    public List<RouteStats> getRoutesByOrigin(String airport, int minFlights)
    {
    	return repo.findRoutesByOrigin(airport.toUpperCase().trim(), minFlights)
    			.stream()
    			.map(row -> new RouteStats(
    					airport.toUpperCase().trim(),
    					(String) row[0],
    					((Number) row[1]).longValue(),
    					round(((Number) row[2]).doubleValue()),
    					round(((Number) row[3]).doubleValue() * 100)
    					)).toList();
    }
}
