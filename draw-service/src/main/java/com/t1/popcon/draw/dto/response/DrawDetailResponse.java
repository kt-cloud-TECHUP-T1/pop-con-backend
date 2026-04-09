package com.t1.popcon.draw.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public record DrawDetailResponse(
        Long drawId,
        LocalDateTime drawOpenAt,
        LocalDateTime drawCloseAt,
        boolean participatable,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") // 밀리초 제외, 초까지만 반환
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
