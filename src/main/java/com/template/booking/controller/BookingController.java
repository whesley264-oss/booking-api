package com.template.booking.controller;

import com.template.booking.dto.*;
import com.template.booking.dto.common.PageRequestDto;
import com.template.booking.dto.common.PageResponse;
import com.template.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Booking management endpoints")
public class BookingController {
    private final BookingService bookingService;

    @PostMapping
    @Operation(summary = "Create a new booking", description = "Creates a new booking for a resource.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Booking created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Resource not found"),
            @ApiResponse(responseCode = "409", description = "Time slot conflict"),
            @ApiResponse(responseCode = "422", description = "Business rule violation")
    })
    public ResponseEntity<BookingResponse> createBooking(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody CreateBookingRequest request) {
        BookingResponse response = bookingService.createBooking(user.getUsername(), request);
        return ResponseEntity.created(URI.create("/api/bookings/" + response.getId())).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get booking by ID")
    public ResponseEntity<BookingResponse> getBooking(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.getBooking(id));
    }

    @GetMapping("/my")
    @Operation(summary = "Get current user's bookings with pagination")
    public ResponseEntity<PageResponse<BookingResponse>> getMyBookings(
            @AuthenticationPrincipal UserDetails user,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "startTime") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "ASC") String sortDirection) {
        PageRequestDto pageRequest = PageRequestDto.builder()
                .page(page).size(size).sortBy(sortBy).sortDirection(sortDirection).build();
        return ResponseEntity.ok(bookingService.getUserBookings(user.getUsername(), pageRequest));
    }

    @GetMapping
    @Operation(summary = "Get all bookings (admin) with pagination")
    public ResponseEntity<PageResponse<BookingResponse>> getAllBookings(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "DESC") String sortDirection) {
        PageRequestDto pageRequest = PageRequestDto.builder()
                .page(page).size(size).sortBy(sortBy).sortDirection(sortDirection).build();
        return ResponseEntity.ok(bookingService.getAllBookings(pageRequest));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel a booking")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking cancelled"),
            @ApiResponse(responseCode = "404", description = "Booking not found"),
            @ApiResponse(responseCode = "422", description = "Cannot cancel")
    })
    public ResponseEntity<BookingResponse> cancelBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        boolean isAdmin = user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(bookingService.cancelBooking(id, user.getUsername(), isAdmin));
    }
}