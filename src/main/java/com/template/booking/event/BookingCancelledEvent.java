package com.template.booking.event;

import com.template.booking.model.Booking;

public class BookingCancelledEvent extends BookingEvent {
    public BookingCancelledEvent(Object source, Booking booking) {
        super(source, booking);
    }
}
