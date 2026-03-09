package com.t1.popcon.popup.banners.service;

import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.popup.dto.card.PhaseStatus;
import com.t1.popcon.popup.dto.card.PhaseType;
import com.t1.popcon.popup.dto.card.PopupCardDto;
import com.t1.popcon.popup.dto.section.PopupSectionResponse;
import com.t1.popcon.popup.dto.section.SectionKey;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PopupBannersService {

    public PopupSectionResponse<PopupCardDto> getBanners(int limit) {

        if (limit < 1 || limit > 5) {
            Map<String, String> errors = new LinkedHashMap<>();
            errors.put("limit", "limit는 1 이상 5 이하여야 합니다.");
            throw new CustomException(ErrorCode.INVALID_INPUT, errors);
        }

        List<PopupCardDto> items = List.of(
            new PopupCardDto(
                102L,
                "하이카와 베이비 팝업",
                "Supporting Text 1",
                null,
                "캡션 문구 1",
                null,
                null,
                null,
                null,
                new PopupCardDto.PhaseDto(
                    PhaseType.DRAW,
                    PhaseStatus.UPCOMING,
                    OffsetDateTime.parse("2026-03-12T10:00:00+09:00"),
                    OffsetDateTime.parse("2026-03-20T20:00:00+09:00")
                )
            ),
            new PopupCardDto(
                103L,
                "성수 팝업",
                "Supporting Text 2",
                null,
                "캡션 문구 2",
                null,
                null,
                null,
                null,
                new PopupCardDto.PhaseDto(
                    PhaseType.AUCTION,
                    PhaseStatus.OPEN,
                    OffsetDateTime.parse("2026-03-03T11:00:00+09:00"),
                    OffsetDateTime.parse("2026-03-08T20:00:00+09:00")
                )
            ),
            new PopupCardDto(
                101L,
                "T1 팝업 스토어",
                "T1 × POPUP SEOUL",
                null,
                "캡션 문구 3",
                null,
                null,
                null,
                null,
                new PopupCardDto.PhaseDto(
                    PhaseType.DRAW,
                    PhaseStatus.OPEN,
                    OffsetDateTime.parse("2026-03-01T10:00:00+09:00"),
                    OffsetDateTime.parse("2026-03-10T20:00:00+09:00")
                )
            ),
            new PopupCardDto(
                104L,
                "스포츠 팝업",
                "Supporting Text 4",
                null,
                "캡션 문구 4",
                null,
                null,
                null,
                null,
                new PopupCardDto.PhaseDto(
                    PhaseType.DRAW,
                    PhaseStatus.OPEN,
                    OffsetDateTime.parse("2026-03-05T10:00:00+09:00"),
                    OffsetDateTime.parse("2026-03-15T20:00:00+09:00")
                )
            ),
            new PopupCardDto(
                105L,
                "아트 스페셜 팝업",
                "Supporting Text 5",
                null,
                "캡션 문구 5",
                null,
                null,
                null,
                null,
                new PopupCardDto.PhaseDto(
                    PhaseType.DRAW,
                    PhaseStatus.UPCOMING,
                    OffsetDateTime.parse("2026-03-18T10:00:00+09:00"),
                    OffsetDateTime.parse("2026-03-25T20:00:00+09:00")
                )
            )
        );

        List<PopupCardDto> result = items.stream()
            .limit(limit)
            .toList();

        return new PopupSectionResponse<>(
            SectionKey.BANNERS,
            result.size(),
            result
        );
    }
}