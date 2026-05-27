package com.template.booking.service;

import com.template.booking.config.BookingProperties;
import com.template.booking.dto.*;
import com.template.booking.dto.common.PageRequestDto;
import com.template.booking.dto.common.PageResponse;
import com.template.booking.event.BookingCancelledEvent;
import com.template.booking.event.BookingCreatedEvent;
import com.template.booking.exception.*;
import com.template.booking.model.Booking;
import com.template.booking.model.Resource;
import com.template.booking.repository.BookingRepository;
import com.template.booking.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {
    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final ResourceRepository resourceRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final BookingProperties bookingProperties;

    @Transactional
    public BookingResponse createBooking(String userId, CreateBookingRequest request) {
        log.info("Creating booking for user: {}, resource: {}, time: {} - {}",
                userId, request.getResourceId(), request.getStartTime(), request.getEndTime());

        validateBookingTimes(request.getStartTime(), request.getEndTime());
        Resource resource = getActiveResource(request.getResourceId());
        checkForConflicts(request.getResourceId(), request.getStartTime(), request.getEndTime());

        Booking booking = Booking.builder()
                .userId(userId)
                .resource(resource)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .reason(request.getReason())
                .status(Booking.BookingStatus.CONFIRMED)
                .build();

        Booking saved = bookingRepository.save(booking);
        log.info("Booking created successfully with ID: {}", saved.getId());

        eventPublisher.publishEvent(new BookingCreatedEvent(this, saved));
        return BookingResponse.fromEntity(saved);
    }

    @Transactional
    public BookingResponse cancelBooking(Long bookingId, String userId, boolean isAdmin) {
        log.info("Cancelling booking: {} by user: {}", bookingId, userId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found: " + bookingId));

        if (!isAdmin && !booking.getUserId().equals(userId)) {
            throw new InvalidBookingException("You are not authorized to cancel this booking");
        }

        if (booking.getStartTime().isBefore(LocalDateTime.now())) {
            throw new InvalidBookingException("Cannot cancel a booking that has already started");
        }

        int minHours = bookingProperties.getCancellation().getMinHours();
        if (booking.getStartTime().isBefore(LocalDateTime.now().plusHours(minHours))) {
            throw new InvalidBookingException(
                    "Cannot cancel booking within " + minHours + " hour(s) of start time");
        }

        booking.setStatus(Booking.BookingStatus.CANCELLED);
        Booking saved = bookingRepository.save(booking);
        log.info("Booking {} cancelled successfully", bookingId);

        eventPublisher.publishEvent(new BookingCancelledEvent(this, saved));
        return BookingResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<AvailabilityResponse> getAvailability(Long resourceId, LocalDate date) {
        log.info("Getting availability for resource: {} on date: {}", resourceId, date);

        getActiveResource(resourceId);
        LocalDateTime dayStart = date.atStartOfDay();

        List<Booking> existingBookings = bookingRepository.findByResourceIdAndDate(resourceId, dayStart);

        List<AvailabilityResponse> slots = new ArrayList<>();
        var businessConfig = bookingProperties.getBusiness();
        
        for (var time = businessConfig.getStartTime(); 
             time.isBefore(businessConfig.getEndTime()); 
             time = time.plusHours(1)) {
            LocalDateTime slotStart = date.atTime(time);
            LocalDateTime slotEnd = date.atTime(time.plusHours(1));

            boolean isBooked = existingBookings.stream()
                    .anyMatch(b -> b.getStartTime().isBefore(slotEnd) && b.getEndTime().isAfter(slotStart));

            slots.add(AvailabilityResponse.builder()
                    .startTime(slotStart)
                    .endTime(slotEnd)
                    .available(!isBooked)
                    .reason(isBooked ? "Already booked" : null)
                    .build());
        }
        return slots;
    }

    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> getUserBookings(String userId, PageRequestDto pageRequest) {
        Page<Booking> page = bookingRepository.findByUserIdAndStatus(
                Booking.BookingStatus.CONFIRMED, pageRequest.toSpringPageRequest());
        List<BookingResponse> content = page.getContent().stream()
                .map(BookingResponse::fromEntity)
                .collect(Collectors.toList());
        return PageResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> getAllBookings(PageRequestDto pageRequest) {
        Page<Booking> page = bookingRepository.findByStatus(
                Booking.BookingStatus.CONFIRMED, pageRequest.toSpringPageRequest());
        List<BookingResponse> content = page.getContent().stream()
                .map(BookingResponse::fromEntity)
                .collect(Collectors.toList());
        return PageResponse.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public BookingResponse getBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found: " + bookingId));
        return BookingResponse.fromEntity(booking);
    }

    private void validateBookingTimes(LocalDateTime startTime, LocalDateTime endTime) {
        var bookingConfig = bookingProperties.getBooking();
        var businessConfig = bookingProperties.getBusiness();

        if (startTime.isAfter(endTime)) {
            throw new InvalidBookingException("Start time must be before end time");
        }
        if (startTime.isBefore(LocalDateTime.now())) {
            throw new InvalidBookingException("Cannot create a booking in the past");
        }
        if (startTime.plusMinutes(bookingConfig.getMinDurationMinutes()).isAfter(endTime)) {
            throw new InvalidBookingException(
                    "Booking must be at least " + bookingConfig.getMinDurationMinutes() + " minutes long");
        }
        if (startTime.toLocalTime().isBefore(businessConfig.getStartTime()) || 
            endTime.toLocalTime().isAfter(businessConfig.getEndTime())) {
            throw new InvalidBookingException(
                    "Booking must be within business hours (" + 
                    businessConfig.getHourStart() + " - " + businessConfig.getHourEnd() + ")");
        }
        if (!bookingConfig.isAllowWeekends() && 
            (startTime.getDayOfWeek() == DayOfWeek.SATURDAY || startTime.getDayOfWeek() == DayOfWeek.SUNDAY)) {
            throw new InvalidBookingException("Booking is not allowed on weekends");
        }
    }

    private Resource getActiveResource(Long resourceId) {
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + resourceId));

        if (resource.getStatus() != Resource.ResourceStatus.ACTIVE) {
            throw new InvalidBookingException("Resource is not active: " + resourceId);
        }
        return resource;
    }

    private void checkForConflicts(Long resourceId, LocalDateTime startTime, LocalDateTime endTime) {
        bookingRepository.findConflictingBooking(resourceId, startTime, endTime)
                .ifPresent(conflict -> {
                    throw new TimeSlotConflictException(
                            String.format("Time slot conflict: resource %d is already booked from %s to %s",
                                    resourceId, conflict.getStartTime(), conflict.getEndTime()));
                });
    }
}