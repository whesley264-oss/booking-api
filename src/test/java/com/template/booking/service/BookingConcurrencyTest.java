package com.template.booking.service;

import com.template.booking.dto.CreateBookingRequest;
import com.template.booking.exception.TimeSlotConflictException;
import com.template.booking.model.Booking;
import com.template.booking.model.Resource;
import com.template.booking.repository.BookingRepository;
import com.template.booking.repository.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test to verify concurrency handling - only one booking should be created
 * when multiple threads try to book the same time slot simultaneously.
 */
class BookingConcurrencyTest {

    private BookingRepository bookingRepository;
    private ResourceRepository resourceRepository;
    private ApplicationEventPublisher eventPublisher;
    private BookingService bookingService;

    private Resource testResource;
    private LocalDateTime bookingTime;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        resourceRepository = mock(ResourceRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);

        bookingService = new BookingService(bookingRepository, resourceRepository, eventPublisher);

        testResource = Resource.builder()
                .id(1L)
                .name("Sala de Reunião")
                .status(Resource.ResourceStatus.ACTIVE)
                .build();

        bookingTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
    }

    @Test
    @DisplayName("Only one booking should succeed when multiple threads try to book same slot")
    void concurrentBooking_OnlyOneShouldSucceed() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        // First call returns no conflict, subsequent calls return conflict
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(testResource));
        
        AtomicInteger callCount = new AtomicInteger(0);
        when(bookingRepository.findConflictingBooking(anyLong(), any(), any()))
                .thenAnswer(inv -> {
                    int count = callCount.incrementAndGet();
                    if (count == 1) {
                        return Optional.empty();
                    }
                    return Optional.of(Booking.builder()
                            .id(99L)
                            .resource(testResource)
                            .startTime(bookingTime)
                            .endTime(bookingTime.plusHours(1))
                            .build());
                });

        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId((long) callCount.get());
            b.setCreatedAt(LocalDateTime.now());
            return b;
        });

        for (int i = 0; i < threadCount; i++) {
            final int threadNum = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    CreateBookingRequest request = CreateBookingRequest.builder()
                            .resourceId(1L)
                            .startTime(bookingTime)
                            .endTime(bookingTime.plusHours(1))
                            .reason("Thread " + threadNum)
                            .build();

                    bookingService.createBooking("user" + threadNum, request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    if (e.getCause() instanceof TimeSlotConflictException) {
                        conflictCount.incrementAndGet();
                    }
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await();
        executor.shutdown();

        assertThat(successCount.get() + conflictCount.get()).isEqualTo(threadCount);
        assertThat(successCount.get()).isEqualTo(1);
    }
}
