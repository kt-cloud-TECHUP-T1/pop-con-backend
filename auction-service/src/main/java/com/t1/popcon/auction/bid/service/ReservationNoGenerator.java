package com.t1.popcon.auction.bid.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class ReservationNoGenerator {

    private static final String PREFIX = "TKT";
    private static final String DIGITS = "0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public String generate() {
        String randomDigits = IntStream.range(0, 8)
                .map(i -> DIGITS.charAt(RANDOM.nextInt(DIGITS.length())))
                .mapToObj(c -> String.valueOf((char) c))
                .collect(Collectors.joining());
        return PREFIX + randomDigits;
    }
}
