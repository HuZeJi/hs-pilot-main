package com.huggingsoft.pilot_main.web.controllers;

import com.huggingsoft.pilot_main.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "APIs for generating reports")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportService reportService; // Assuming ReportService

    // Placeholder for getting the authenticated user's principal
    private Object getCurrentUserPrincipal() {
        return new Object(); // Dummy object
    }

    @GetMapping("/sales")
    @Operation(summary = "Generate a Sales Report")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Report generated successfully",
                    content = @Content(mediaType = "application/json")), // Adjust content type based on actual response
            @ApiResponse(responseCode = "400", description = "Invalid query parameters"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @Parameter(in = ParameterIn.QUERY, name = "dateFrom", description = "Start date (ISO 8601)", required = true, schema = @Schema(type="string", format="date-time"))
    @Parameter(in = ParameterIn.QUERY, name = "dateTo", description = "End date (ISO 8601)", required = true, schema = @Schema(type="string", format="date-time"))
    @Parameter(in = ParameterIn.QUERY, name = "groupBy", description = "Grouping dimension (e.g., 'client', 'product', 'day')", schema = @Schema(type="string"))
    public ResponseEntity<?> generateSalesReport( // Using wildcard for flexible response type (JSON, CSV etc)
                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateFrom,
                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateTo,
                                                  @RequestParam(required = false) String groupBy) {

        // The service method would determine the actual return type/format
        Object reportData = reportService.generateSalesReport(getCurrentUserPrincipal(), dateFrom, dateTo, groupBy);
        // Determine content type based on requested format or default to JSON
        // HttpHeaders headers = new HttpHeaders();
        // headers.setContentType(MediaType.APPLICATION_JSON); // Or APPLICATION_PDF, etc.
        // return new ResponseEntity<>(reportData, headers, HttpStatus.OK);
        return ResponseEntity.ok(reportData); // Simplified for JSON default
    }

    @GetMapping("/inventory")
    @Operation(summary = "Generate an Inventory Report")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Report generated successfully",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @Parameter(in = ParameterIn.QUERY, name = "category", description = "Filter by category", schema = @Schema(type="string"))
    @Parameter(in = ParameterIn.QUERY, name = "minStock", description = "Filter by minimum stock level", schema = @Schema(type="integer"))
    @Parameter(in = ParameterIn.QUERY, name = "maxStock", description = "Filter by maximum stock level", schema = @Schema(type="integer"))
    public ResponseEntity<?> generateInventoryReport(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer minStock,
            @RequestParam(required = false) Integer maxStock) {

        Object reportData = reportService.generateInventoryReport(getCurrentUserPrincipal(), category, minStock, maxStock);
        return ResponseEntity.ok(reportData);
    }

    // Add similar endpoints for Purchase Reports etc.
}
