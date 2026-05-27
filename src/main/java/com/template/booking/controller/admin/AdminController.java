package com.template.booking.controller.admin;

import com.template.booking.dto.common.PageRequestDto;
import com.template.booking.dto.common.PageResponse;
import com.template.booking.dto.BookingResponse;
import com.template.booking.dto.ResourceResponse;
import com.template.booking.service.BookingService;
import com.template.booking.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin dashboard and statistics")
@SecurityRequirement(name = "basicAuth")
public class AdminController {
    private final ResourceService resourceService;
    private final BookingService bookingService;

    @GetMapping("/stats")
    @Operation(summary = "Get dashboard statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        
        PageResponse<ResourceResponse> activeResources = resourceService.getActiveResources(
                PageRequestDto.builder().page(0).size(1).build());
        PageResponse<ResourceResponse> allResources = resourceService.getAllResources(
                PageRequestDto.builder().page(0).size(1).build());
        PageResponse<BookingResponse> allBookings = bookingService.getAllBookings(
                PageRequestDto.builder().page(0).size(1).build());
        
        stats.put("totalResources", allResources.getTotalElements());
        stats.put("activeResources", activeResources.getTotalElements());
        stats.put("inactiveResources", allResources.getTotalElements() - activeResources.getTotalElements());
        stats.put("totalBookings", allBookings.getTotalElements());
        
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/bookings")
    @Operation(summary = "Get all bookings with pagination (admin)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<BookingResponse>> getAllBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        PageRequestDto pageRequest = PageRequestDto.builder()
                .page(page).size(size).sortBy(sortBy).sortDirection(sortDirection).build();
        return ResponseEntity.ok(bookingService.getAllBookings(pageRequest));
    }
}