package com.template.booking.service;

import com.template.booking.dto.AvailabilityResponse;
import com.template.booking.dto.BookingResponse;
import com.template.booking.dto.CreateBookingRequest;
import com.template.booking.exception.*;
import com.template.booking.model.Booking;
import com.template.booking.model.Resource;
import com.template.booking.repository.BookingRepository;
import com.template.booking.repository.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private ResourceRepository resourceRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private BookingService bookingService;

    private Resource testResource;
    private CreateBookingRequest validRequest;

    @BeforeEach
    void setUp() {
        testResource = Resource.builder()
                .id(1L)
                .name("Sala de Reunião A")
                .status(Resource.ResourceStatus.ACTIVE)
                .build();

        validRequest = CreateBookingRequest.builder()
                .resourceId(1L)
                .startTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0))
                .endTime(LocalDateTime.now().plusDays(1).withHour(11).withMinute(0))
                .reason("Reunião de equipe")
                .build();
    }

    @Test
    @DisplayName("Should create booking successfully")
    void createBooking_Success() {
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(testResource));
        when(bookingRepository.findConflictingBooking(anyLong(), any(), any())).thenReturn(Optional.empty());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(1L);
            b.setCreatedAt(LocalDateTime.now());
            return b;
        });

        BookingResponse response = bookingService.createBooking("user1", validRequest);

        assertThat(response).isNotNull();
        assertThat(response.getResourceId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo("CONFIRMED");
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    @DisplayName("Should throw exception when time slot has conflict")
    void createBooking_Conflict() {
        Booking existingBooking = Booking.builder()
                .id(99L)
                .resource(testResource)
                .startTime(validRequest.getStartTime().minusHours(1))
                .endTime(validRequest.getEndTime().plusHours(1))
                .build();

        when(resourceRepository.findById(1L)).thenReturn(Optional.of(testResource));
        when(bookingRepository.findConflictingBooking(anyLong(), any(), any()))
                .thenReturn(Optional.of(existingBooking));

        assertThatThrownBy(() -> bookingService.createBooking("user1", validRequest))
                .isInstanceOf(TimeSlotConflictException.class)
                .hasMessageContaining("Time slot conflict");
    }

    @Test
    @DisplayName("Should throw exception when start time is in the past")
    void createBooking_PastTime() {
        CreateBookingRequest pastRequest = CreateBookingRequest.builder()
                .resourceId(1L)
                .startTime(LocalDateTime.now().minusHours(2))
                .endTime(LocalDateTime.now().minusHours(1))
                .build();

        assertThatThrownBy(() -> bookingService.createBooking("user1", pastRequest))
                .isInstanceOf(InvalidBookingException.class)
                .hasMessageContaining("past");
    }

    @Test
    @DisplayName("Should throw exception when start time is after end time")
    void createBooking_InvalidTimes() {
        CreateBookingRequest invalidRequest = CreateBookingRequest.builder()
                .resourceId(1L)
                .startTime(LocalDateTime.now().plusDays(1).withHour(14))
                .endTime(LocalDateTime.now().plusDays(1).withHour(10))
                .build();

        assertThatThrownBy(() -> bookingService.createBooking("user1", invalidRequest))
                .isInstanceOf(InvalidBookingException.class)
                .hasMessageContaining("before");
    }

    @Test
    @DisplayName("Should throw exception when resource is not found")
    void createBooking_ResourceNotFound() {
        when(resourceRepository.findById(99L)).thenReturn(Optional.empty());

        CreateBookingRequest request = CreateBookingRequest.builder()
                .resourceId(99L)
                .startTime(LocalDateTime.now().plusDays(1).withHour(10))
                .endTime(LocalDateTime.now().plusDays(1).withHour(11))
                .build();

        assertThatThrownBy(() -> bookingService.createBooking("user1", request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw exception when resource is inactive")
    void createBooking_InactiveResource() {
        Resource inactiveResource = Resource.builder()
                .id(2L)
                .name("Sala Inativa")
                .status(Resource.ResourceStatus.INACTIVE)
                .build();

        when(resourceRepository.findById(2L)).thenReturn(Optional.of(inactiveResource));

        CreateBookingRequest request = CreateBookingRequest.builder()
                .resourceId(2L)
                .startTime(LocalDateTime.now().plusDays(1).withHour(10))
                .endTime(LocalDateTime.now().plusDays(1).withHour(11))
                .build();

        assertThatThrownBy(() -> bookingService.createBooking("user1", request))
                .isInstanceOf(InvalidBookingException.class)
                .hasMessageContaining("not active");
    }

    @Test
    @DisplayName("Should cancel booking successfully")
    void cancelBooking_Success() {
        Booking booking = Booking.builder()
                .id(1L)
                .userId("user1")
                .resource(testResource)
                .startTime(LocalDateTime.now().plusHours(5))
                .endTime(LocalDateTime.now().plusHours(6))
                .status(Booking.BookingStatus.CONFIRMED)
                .build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        BookingResponse response = bookingService.cancelBooking(1L, "user1", false);

        assertThat(response.getStatus()).isEqualTo("CANCELLED");
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    @DisplayName("Should throw exception when cancelling booking within 1 hour")
    void cancelBooking_TooLate() {
        Booking booking = Booking.builder()
                .id(1L)
                .userId("user1")
                .resource(testResource)
                .startTime(LocalDateTime.now().plusMinutes(30))
                .endTime(LocalDateTime.now().plusHours(1).plusMinutes(30))
                .status(Booking.BookingStatus.CONFIRMED)
                .build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking(1L, "user1", false))
                .isInstanceOf(InvalidBookingException.class)
                .hasMessageContaining("1 hour");
    }

    @Test
    @DisplayName("Should allow admin to cancel any booking")
    void cancelBooking_AdminCanCancelOthers() {
        Booking booking = Booking.builder()
                .id(1L)
                .userId("otherUser")
                .resource(testResource)
                .startTime(LocalDateTime.now().plusHours(3))
                .endTime(LocalDateTime.now().plusHours(4))
                .status(Booking.BookingStatus.CONFIRMED)
                .build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        BookingResponse response = bookingService.cancelBooking(1L, "admin", true);

        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("Should return availability slots")
    void getAvailability_Success() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(testResource));
        when(bookingRepository.findByResourceIdAndDate(eq(1L), any())).thenReturn(List.of());

        List<AvailabilityResponse> slots = bookingService.getAvailability(1L, tomorrow);

        assertThat(slots).isNotEmpty();
        assertThat(slots).hasSize(10);
        assertThat(slots).allMatch(slot -> slot.isAvailable());
    }
}
