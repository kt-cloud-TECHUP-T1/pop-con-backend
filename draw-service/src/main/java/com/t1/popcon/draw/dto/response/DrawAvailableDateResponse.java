package com.t1.popcon.draw.dto.response;

import java.time.LocalDate;

public record DrawAvailableDateResponse(
    LocalDate entryDate
) {
}