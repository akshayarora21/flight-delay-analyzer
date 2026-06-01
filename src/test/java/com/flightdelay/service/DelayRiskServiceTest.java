package com.flightdelay.service;

import com.flightdelay.dto.Responses.DelayRiskResponse;
import com.flightdelay.model.Flight;
import com.flightdelay.repository.FlightRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DelayRiskServiceTest {

    @Mock
    private FlightRepository repo;

    @InjectMocks
    private DelayRiskService service;

    private List<Flight> buildFlights(int count, double arrDelay, boolean cancelled) {
        List<Flight> flights = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Flight f = new Flight();
            f.setOrigin("ORD");
            f.setDest("LAX");
            f.setCarrier("AA");
            f.setMonth(6);
            f.setYear(2023);
            f.setDayOfWeek(2);
            f.setCancelled(cancelled);
            if (!cancelled) f.setArrivalDelay(arrDelay);
            flights.add(f);
        }
        return flights;
    }

    @BeforeEach
    void stubFallback() {
        // Default: return empty for specific queries so fallback is used
        when(repo.findByRouteCarrierAndMonth(anyString(), anyString(), anyString(), anyInt()))
            .thenReturn(Collections.emptyList());
        when(repo.findByRouteAndCarrier(anyString(), anyString(), anyString()))
            .thenReturn(Collections.emptyList());
    }

    @Test
    void lowRisk_whenFlightsAreOnTime() {
        List<Flight> onTime = buildFlights(50, -5.0, false);
        when(repo.findByRoute("ORD", "LAX")).thenReturn(onTime);

        DelayRiskResponse r = service.predictRisk("ORD", "LAX", null, null);

        assertThat(r.riskLabel()).isEqualTo("LOW");
        assertThat(r.delayRatePercent()).isEqualTo(0.0);
        assertThat(r.sampleSize()).isEqualTo(50);
    }

    @Test
    void highRisk_whenFlightsAreFrequentlyLate() {
        List<Flight> late = buildFlights(50, 45.0, false);
        when(repo.findByRoute("ORD", "LAX")).thenReturn(late);

        DelayRiskResponse r = service.predictRisk("ORD", "LAX", null, null);

        assertThat(r.riskLabel()).isIn("HIGH", "VERY HIGH");
        assertThat(r.delayRatePercent()).isEqualTo(100.0);
    }

    @Test
    void unknownRisk_whenInsufficientData() {
        when(repo.findByRoute("XYZ", "ABC")).thenReturn(Collections.emptyList());

        DelayRiskResponse r = service.predictRisk("XYZ", "ABC", null, null);

        assertThat(r.riskLabel()).isEqualTo("UNKNOWN");
        assertThat(r.riskScore()).isEqualTo(-1.0);
    }

    @Test
    void originAndDestAreNormalized() {
        List<Flight> flights = buildFlights(20, 5.0, false);
        when(repo.findByRoute("ORD", "LAX")).thenReturn(flights);

        // Should normalize to uppercase
        DelayRiskResponse r = service.predictRisk("ord", "lax", null, null);

        assertThat(r.origin()).isEqualTo("ORD");
        assertThat(r.dest()).isEqualTo("LAX");
    }

    @Test
    void cancellationRate_calculatedCorrectly() {
        List<Flight> mixed = new ArrayList<>();
        mixed.addAll(buildFlights(8, 10.0, false));
        mixed.addAll(buildFlights(2, 0.0, true));   // 20% cancelled
        when(repo.findByRoute("ORD", "LAX")).thenReturn(mixed);

        DelayRiskResponse r = service.predictRisk("ORD", "LAX", null, null);

        assertThat(r.cancellationRatePercent()).isEqualTo(20.0);
        assertThat(r.sampleSize()).isEqualTo(10);
    }

    @Test
    void riskScore_inRange0to100() {
        List<Flight> veryLate = buildFlights(30, 90.0, false);
        when(repo.findByRoute("ORD", "LAX")).thenReturn(veryLate);

        DelayRiskResponse r = service.predictRisk("ORD", "LAX", null, null);

        assertThat(r.riskScore()).isBetween(0.0, 100.0);
    }
}
