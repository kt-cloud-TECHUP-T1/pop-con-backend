package com.t1.popcon.ticket.service;

import org.springframework.stereotype.Component;

@Component
public class TicketNumberGenerator {

    private static final String PREFIX = "TKT";
    private static final int NUMBER_WIDTH = 8;

    public String generate(Long ticketId) {
        return PREFIX + String.format("%0" + NUMBER_WIDTH + "d", ticketId);
    }
}
