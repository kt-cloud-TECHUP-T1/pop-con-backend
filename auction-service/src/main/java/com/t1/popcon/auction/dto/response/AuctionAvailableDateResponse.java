package com.t1.popcon.auction.dto.response;

import java.time.LocalDate;

public record AuctionAvailableDateResponse(
    LocalDate auctionDate
) {
}