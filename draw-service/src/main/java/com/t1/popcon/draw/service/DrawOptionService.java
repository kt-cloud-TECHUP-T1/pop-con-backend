package com.t1.popcon.draw.service;

import com.t1.popcon.draw.domain.Draw;
import com.t1.popcon.draw.domain.DrawOption;
import com.t1.popcon.draw.dto.response.DrawAvailableDateResponse;
import com.t1.popcon.draw.dto.response.DrawOptionResponse;
import com.t1.popcon.draw.repository.DrawOptionRepository;
import com.t1.popcon.draw.repository.DrawRepository;
import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DrawOptionService {

    private final DrawRepository drawRepository;
    private final DrawOptionRepository drawOptionRepository;

    // 날짜 목록 조회
    public List<DrawAvailableDateResponse> getAvailableDates(Long drawId) {
        Draw Draw = getSelectableDraw(drawId);

        return drawOptionRepository.findByDraw_IdOrderByEntryDateAscEntryTimeAsc(Draw.getId())
            .stream()
            .map(DrawOption::getEntryDate)
            .distinct()
            .map(DrawAvailableDateResponse::new)
            .toList();
    }

    // 날짜별 회차 조회
    public List<DrawOptionResponse> getOptionsByDate(Long drawId, LocalDate entryDate) {
        Draw draw = getSelectableDraw(drawId);

        return drawOptionRepository.findByDraw_IdAndEntryDateOrderByEntryTimeAsc(draw.getId(), entryDate)
            .stream()
            .map(option -> new DrawOptionResponse(
                option.getId(),
                option.getEntryTime()
            ))
            .toList();
    }

    private Draw getSelectableDraw(Long drawId) {
        Draw draw = drawRepository.findById(drawId)
            .orElseThrow(() -> new CustomException(ErrorCode.DRAW_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();

        if (now.isAfter(draw.getDrawCloseAt())) {
            throw new CustomException(ErrorCode.DRAW_ALREADY_CLOSED);
        }

        if (now.isBefore(draw.getDrawOpenAt())) {
            throw new CustomException(ErrorCode.DRAW_NOT_OPEN);
        }

        return draw;
    }
}