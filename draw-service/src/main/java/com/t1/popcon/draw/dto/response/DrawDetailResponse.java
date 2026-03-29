package com.t1.popcon.draw.dto.response;

import java.time.LocalDateTime;

public record DrawDetailResponse(
        Long drawId,
        LocalDateTime drawOpenAt,
        LocalDateTime drawCloseAt,
        boolean participatable,
        LocalDateTime serverTime
) {
    public static DrawDetailResponse of(
            Long drawId,
            LocalDateTime drawOpenAt,
            LocalDateTime drawCloseAt,
            boolean participatable,
            LocalDateTime serverTime
    ) {
        return new DrawDetailResponse(drawId, drawOpenAt, drawCloseAt, participatable, serverTime);
    }
}
