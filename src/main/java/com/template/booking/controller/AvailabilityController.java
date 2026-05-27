package com.template.booking.controller;

import com.template.booking.dto.AvailabilityResponse;
import com.template.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/availability")
@RequiredArgsConstructor
@Tag(name = "Availability", description = "Resource availability endpoints")
public class AvailabilityController {
    private final BookingService bookingService;

    @GetMapping("/resource/{resourceId}")
    @Operation(summary = "Get resource availability for a date",
            description = "Returns hourly slots showing which times are available for booking. " +
                    "Assumes resource is available 08:00-18:00.")
    public ResponseEntity<List<AvailabilityResponse>> getResourceAvailability(
            @Parameter(description = "Resource ID") @PathVariable Long resourceId,
            @Parameter(description = "Date to check (YYYY-MM-DD)") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(bookingService.getAvailability(resourceId, date));
    }
}
