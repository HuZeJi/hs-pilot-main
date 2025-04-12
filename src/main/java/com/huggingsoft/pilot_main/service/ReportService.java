package com.huggingsoft.pilot_main.service;

import java.time.OffsetDateTime;

// --- Report Service ---
public interface ReportService {
    // Return type can be specific DTOs, Map, or even byte[] for file downloads
    Object generateSalesReport(Object principal, OffsetDateTime dateFrom, OffsetDateTime dateTo, String groupBy);
    Object generateInventoryReport(Object principal, String category, Integer minStock, Integer maxStock);
    // Add other report methods as needed
}
