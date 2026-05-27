package com.template.booking.service;

import com.template.booking.model.Booking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Async
    public void sendBookingConfirmation(Booking booking) {
        log.info("Sending booking confirmation email to user: {}", booking.getUserId());
        log.info("   Booking ID: {}, Resource: {}, Time: {} - {}",
                booking.getId(), 
                booking.getResource().getName(),
                booking.getStartTime(),
                booking.getEndTime());
        log.info("   [Email sent successfully]");
    }

    @Async
    public void sendBookingCancellation(Booking booking) {
        log.info("Sending booking cancellation email to user: {}", booking.getUserId());
        log.info("   Booking ID: {}, Resource: {}, Time: {} - {}",
                booking.getId(),
                booking.getResource().getName(),
                booking.getStartTime(),
                booking.getEndTime());
        log.info("   [Cancellation email sent successfully]");
    }
}
