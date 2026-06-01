package com.flightdelay.ingestion;

import com.flightdelay.model.Flight;
import com.flightdelay.repository.FlightRepository;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads BTS On-Time Performance CSV data at startup.
 *
 * Download data from:
 *   https://www.transtats.bts.gov/DL_SelectFields.aspx?gnoyr_VQ=FGK
 *
 * Select these fields (at minimum):
 *   YEAR, MONTH, DAY_OF_WEEK, OP_UNIQUE_CARRIER, ORIGIN, DEST,
 *   DEP_DELAY, ARR_DELAY, CANCELLED,
 *   CARRIER_DELAY, WEATHER_DELAY, NAS_DELAY, SECURITY_DELAY, LATE_AIRCRAFT_DELAY
 *
 * Save the CSV to: src/main/resources/bts_flights.csv
 */
@Component
public class BtsDataIngestionService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BtsDataIngestionService.class);
    private static final int BATCH_SIZE = 1000;

    @Value("${app.bts.data-path}")
    private String dataPath;

    @Value("${app.bts.max-rows}")
    private int maxRows;

    private final FlightRepository repo;

    public BtsDataIngestionService(FlightRepository repo) {
        this.repo = repo;
    }

    @Override
    public void run(String... args) {
        File file = new File(dataPath);
        if (!file.exists()) {
            log.warn("BTS data file not found at '{}'. " +
                     "Download from https://www.transtats.bts.gov and place at that path. " +
                     "API will start with empty database.", dataPath);
            return;
        }

        log.info("Loading BTS flight data from {}...", dataPath);
        long start = System.currentTimeMillis();
        int loaded = loadCsv(file);
        long elapsed = System.currentTimeMillis() - start;

        log.info("Loaded {} flight records in {}ms. Total in DB: {}",
                 loaded, elapsed, repo.count());
    }

    private int loadCsv(File file) {
        List<Flight> batch = new ArrayList<>(BATCH_SIZE);
        int count = 0;
        int skipped = 0;

        try (CSVReader reader = new CSVReader(new FileReader(file))) {
            String[] header = reader.readNext();
            if (header == null) {
                log.error("CSV file is empty");
                return 0;
            }

            // Build column index map (handles different BTS column orderings)
            ColumnMap cols = new ColumnMap(header);
            log.info("CSV columns detected: {}", cols);

            String[] row;
            while ((row = reader.readNext()) != null) {
                if (maxRows > 0 && count >= maxRows) break;

                try {
                    Flight f = parseRow(row, cols);
                    batch.add(f);
                    count++;

                    if (batch.size() >= BATCH_SIZE) {
                        repo.saveAll(batch);
                        batch.clear();
                        if (count % 10_000 == 0) {
                            log.info("  ...loaded {} rows", count);
                        }
                    }
                } catch (Exception e) {
                    skipped++;
                    if (skipped <= 5) {
                        log.warn("Skipping unparseable row {}: {}", count + skipped, e.getMessage());
                    }
                }
            }

            if (!batch.isEmpty()) repo.saveAll(batch);

        } catch (IOException | CsvValidationException e) {
            log.error("Failed to read CSV: {}", e.getMessage());
        }

        if (skipped > 0) log.warn("Skipped {} unparseable rows total", skipped);
        return count;
    }

    private Flight parseRow(String[] row, ColumnMap cols) {
        Flight f = new Flight();
        f.setYear(parseInt(row, cols.year));
        f.setMonth(parseInt(row, cols.month));
        f.setDayOfWeek(parseInt(row, cols.dayOfWeek));
        f.setCarrier(clean(row, cols.carrier));
        f.setOrigin(clean(row, cols.origin));
        f.setDest(clean(row, cols.dest));
        f.setDepartureDelay(parseDouble(row, cols.depDelay));
        f.setArrivalDelay(parseDouble(row, cols.arrDelay));
        f.setCancelled("1".equals(clean(row, cols.cancelled)) ||
                       "1.00".equals(clean(row, cols.cancelled)));
        f.setCarrierDelay(parseDouble(row, cols.carrierDelay));
        f.setWeatherDelay(parseDouble(row, cols.weatherDelay));
        f.setNasDelay(parseDouble(row, cols.nasDelay));
        f.setSecurityDelay(parseDouble(row, cols.securityDelay));
        f.setLateAircraftDelay(parseDouble(row, cols.lateAircraftDelay));
        return f;
    }

    // --- Parsing helpers ---

    private int parseInt(String[] row, int col) {
        if (col < 0 || col >= row.length) return 0;
        String v = row[col].trim().replace("\"", "");
        try { return Integer.parseInt(v); }
        catch (NumberFormatException e) { return 0; }
    }

    private Double parseDouble(String[] row, int col) {
        if (col < 0 || col >= row.length) return null;
        String v = row[col].trim().replace("\"", "");
        if (v.isEmpty() || v.equals("NA")) return null;
        try { return Double.parseDouble(v); }
        catch (NumberFormatException e) { return null; }
    }

    private String clean(String[] row, int col) {
        if (col < 0 || col >= row.length) return "";
        return row[col].trim().replace("\"", "");
    }

    /**
     * Maps known BTS column names to their indices.
     * Handles quoted and unquoted headers.
     */
    private static class ColumnMap {
        int year = -1, month = -1, dayOfWeek = -1;
        int carrier = -1, origin = -1, dest = -1;
        int depDelay = -1, arrDelay = -1, cancelled = -1;
        int carrierDelay = -1, weatherDelay = -1, nasDelay = -1;
        int securityDelay = -1, lateAircraftDelay = -1;

        ColumnMap(String[] header) {
        	for (int i = 0; i < header.length; i++) {
        		String h = header[i].trim().replace("\"", "").toUpperCase();
        		switch (h) {
        		case "YEAR"                -> year = i;
        		case "MONTH"               -> month = i;
        		case "DAY_OF_WEEK"         -> dayOfWeek = i;
        		case "OP_UNIQUE_CARRIER",
        		"OP_CARRIER",
        		"UNIQUE_CARRIER",
        		"CARRIER",
        		"AIRLINE_CODE",
        		"AIRLINE"             -> carrier = i;
        		case "ORIGIN"              -> origin = i;
        		case "DEST"                -> dest = i;
        		case "DEP_DELAY",
        		"DEP_DELAY_NEW"       -> depDelay = i;
        		case "ARR_DELAY",
        		"ARR_DELAY_NEW"       -> arrDelay = i;
        		case "CANCELLED"           -> cancelled = i;
        		case "CARRIER_DELAY",
        		"DELAY_DUE_CARRIER"               -> carrierDelay = i;
        		case "WEATHER_DELAY",
        		"DELAY_DUE_WEATHER"               -> weatherDelay = i;
        		case "NAS_DELAY",
        		"DELAY_DUE_NAS"                   -> nasDelay = i;
        		case "SECURITY_DELAY",
        		"DELAY_DUE_SECURITY"              -> securityDelay = i;
        		case "LATE_AIRCRAFT_DELAY",
        		"DELAY_DUE_LATE_AIRCRAFT"         -> lateAircraftDelay = i;
                }
            }
        }

        @Override
        public String toString() {
            return String.format("year=%d month=%d carrier=%d origin=%d dest=%d " +
                                 "depDelay=%d arrDelay=%d cancelled=%d",
                                 year, month, carrier, origin, dest,
                                 depDelay, arrDelay, cancelled);
        }
    }
}
