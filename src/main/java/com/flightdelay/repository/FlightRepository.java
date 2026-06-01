package com.flightdelay.repository;

import com.flightdelay.model.Flight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlightRepository extends JpaRepository<Flight, Long> {

    // --- Route queries ---

    @Query("""
        SELECT f FROM Flight f
        WHERE f.origin = :origin AND f.dest = :dest
        AND f.cancelled = false
    """)
    List<Flight> findByRoute(@Param("origin") String origin,
                             @Param("dest") String dest);

    @Query("""
        SELECT f FROM Flight f
        WHERE f.origin = :origin AND f.dest = :dest
        AND f.carrier = :carrier
        AND f.cancelled = false
    """)
    List<Flight> findByRouteAndCarrier(@Param("origin") String origin,
                                       @Param("dest") String dest,
                                       @Param("carrier") String carrier);

    @Query("""
        SELECT f FROM Flight f
        WHERE f.origin = :origin AND f.dest = :dest
        AND f.carrier = :carrier
        AND f.month = :month
        AND f.cancelled = false
    """)
    List<Flight> findByRouteCarrierAndMonth(@Param("origin") String origin,
                                             @Param("dest") String dest,
                                             @Param("carrier") String carrier,
                                             @Param("month") int month);

    // --- Aggregation queries ---

    @Query("""
        SELECT f.origin, f.dest,
               COUNT(f) as totalFlights,
               AVG(f.arrivalDelay) as avgDelay,
               SUM(CASE WHEN f.arrivalDelay > 15 THEN 1 ELSE 0 END) * 1.0 / COUNT(f) as delayRate
        FROM Flight f
        WHERE f.cancelled = false
        GROUP BY f.origin, f.dest
        HAVING COUNT(f) >= :minFlights
        ORDER BY avgDelay DESC
    """)
    List<Object[]> findWorstRoutes(@Param("minFlights") int minFlights);

    @Query("""
        SELECT f.carrier,
               COUNT(f) as totalFlights,
               AVG(f.arrivalDelay) as avgDelay,
               SUM(CASE WHEN f.arrivalDelay > 15 THEN 1 ELSE 0 END) * 1.0 / COUNT(f) as delayRate,
               SUM(CASE WHEN f.cancelled = true THEN 1 ELSE 0 END) * 1.0 / COUNT(f) as cancelRate
        FROM Flight f
        GROUP BY f.carrier
        ORDER BY avgDelay DESC
    """)
    List<Object[]> findAirlineStats();

    @Query("""
        SELECT f.origin,
               COUNT(f) as totalFlights,
               AVG(f.departureDelay) as avgDepDelay,
               SUM(CASE WHEN f.departureDelay > 15 THEN 1 ELSE 0 END) * 1.0 / COUNT(f) as depDelayRate
        FROM Flight f
        WHERE f.cancelled = false
        GROUP BY f.origin
        HAVING COUNT(f) >= :minFlights
        ORDER BY avgDepDelay DESC
    """)
    
    List<Object[]> findWorstAirports(@Param("minFlights") int minFlights);
    
    @Query("""
    		SELECT f.dest,
    		COUNT(f) as totalFlights,
    		AVG(f.arrivalDelay) as avgDelay,
    		SUM(CASE WHEN f.arrivalDelay >15 THEN 1 ELSE 0 END) *1.0 / COUNT(f) as
    		FROM Flight f
    		WHERE f.origin = :airport
    		AND f.cancelled = false
    		GROUP BY f.dest
    		HAVING COUNT(f) >= :minFlights
    		ORDER BY avgDelay DESC""")
	List<Object[]> findRoutesByOrigin(@Param("airport") String airport, @Param("minFlights") int minFlights);

    long countByOriginAndDest(String origin, String dest);
}
