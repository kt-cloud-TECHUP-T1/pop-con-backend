package com.t1.popcon.draw.service;

import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.draw.domain.Draw;
import com.t1.popcon.draw.dto.response.DrawDetailResponse;
import com.t1.popcon.draw.repository.DrawRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DrawService {

    private final DrawRepository drawRepository;
    private final Clock clock;

    public DrawDetailResponse getDrawDetail(Long drawId) {
        Draw draw = drawRepository.findById(drawId)
                .orElseThrow(() -> new CustomException(ErrorCode.DRAW_NOT_FOUND));

        LocalDateTime serverTime = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS); // 밀리초 제거

        boolean participatable =
                !serverTime.isBefore(draw.getDrawOpenAt()) &&
                        !serverTime.isAfter(draw.getDrawCloseAt());

        return DrawDetailResponse.of(
                draw.getId(),
                draw.getDrawOpenAt(),
                draw.getDrawCloseAt(),
                participatable,
                serverTime
        );
    }
}
