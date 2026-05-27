package com.template.booking.event;

import com.template.booking.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingEventListener {
    private static final Logger log = LoggerFactory.getLogger(BookingEventListener.class);
    private final NotificationService notificationService;

    @EventListener
    public void handleBookingCreated(BookingCreatedEvent event) {
        log.info("Received BookingCreatedEvent for booking ID: {}", event.getBooking().getId());
        notificationService.sendBookingConfirmation(event.getBooking());
    }

    @EventListener
    public void handleBookingCancelled(BookingCancelledEvent event) {
        log.info("Received BookingCancelledEvent for booking ID: {}", event.getBooking().getId());
        notificationService.sendBookingCancellation(event.getBooking());
    }
}
