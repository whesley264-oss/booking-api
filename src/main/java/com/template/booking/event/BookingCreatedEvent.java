package com.template.booking.event;

import com.template.booking.model.Booking;

public class BookingCreatedEvent extends BookingEvent {
    public BookingCreatedEvent(Object source, Booking booking) {
        super(source, booking);
    }
}
