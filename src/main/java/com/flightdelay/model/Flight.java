package com.flightdelay.model;

import jakarta.persistence.*;

/**
 * Represents a single historical flight record from BTS data.
 *
 * BTS CSV columns used:
 *   YEAR, MONTH, DAY_OF_WEEK, OP_UNIQUE_CARRIER, ORIGIN, DEST,
 *   DEP_DELAY, ARR_DELAY, CANCELLED, CARRIER_DELAY, WEATHER_DELAY,
 *   NAS_DELAY, SECURITY_DELAY, LATE_AIRCRAFT_DELAY
 */
@Entity
@Table(name = "flights", indexes = {
    @Index(name = "idx_origin_dest", columnList = "origin,dest"),
    @Index(name = "idx_carrier", columnList = "carrier"),
    @Index(name = "idx_month", columnList = "\"month\"")
})
public class Flight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "\"month\"")
    private int month;
    
    @Column(name = "\"year\"")
    private int year;
    
    private int dayOfWeek;

    @Column(length = 10)
    private String carrier;

    @Column(length = 5)
    private String origin;

    @Column(length = 5)
    private String dest;

    private Double departureDelay;   // minutes; negative = early
    private Double arrivalDelay;     // minutes; negative = early
    private boolean cancelled;

    private Double carrierDelay;
    private Double weatherDelay;
    private Double nasDelay;
    private Double securityDelay;
    private Double lateAircraftDelay;

    public Flight() {}

    // --- Getters and Setters ---

    public Long getId() { return id; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }

    public int getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(int dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public String getCarrier() { return carrier; }
    public void setCarrier(String carrier) { this.carrier = carrier; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getDest() { return dest; }
    public void setDest(String dest) { this.dest = dest; }

    public Double getDepartureDelay() { return departureDelay; }
    public void setDepartureDelay(Double departureDelay) { this.departureDelay = departureDelay; }

    public Double getArrivalDelay() { return arrivalDelay; }
    public void setArrivalDelay(Double arrivalDelay) { this.arrivalDelay = arrivalDelay; }

    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    public Double getCarrierDelay() { return carrierDelay; }
    public void setCarrierDelay(Double carrierDelay) { this.carrierDelay = carrierDelay; }

    public Double getWeatherDelay() { return weatherDelay; }
    public void setWeatherDelay(Double weatherDelay) { this.weatherDelay = weatherDelay; }

    public Double getNasDelay() { return nasDelay; }
    public void setNasDelay(Double nasDelay) { this.nasDelay = nasDelay; }

    public Double getSecurityDelay() { return securityDelay; }
    public void setSecurityDelay(Double securityDelay) { this.securityDelay = securityDelay; }

    public Double getLateAircraftDelay() { return lateAircraftDelay; }
    public void setLateAircraftDelay(Double lateAircraftDelay) { this.lateAircraftDelay = lateAircraftDelay; }
}
