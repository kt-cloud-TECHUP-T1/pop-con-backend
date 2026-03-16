package com.t1.popcon.draw.dto.response;

import java.time.LocalDateTime;

public record DrawDetailResponse(
        LocalDateTime drawOpenAt,
        LocalDateTime drawCloseAt,
        boolean participatable,
        LocalDateTime serverTime
) {
    public static DrawDetailResponse of(
            LocalDateTime drawOpenAt,
            LocalDateTime drawCloseAt,
            boolean participatable,
            LocalDateTime serverTime
    ) {
        return new DrawDetailResponse(drawOpenAt, drawCloseAt, participatable, serverTime);
    }
}