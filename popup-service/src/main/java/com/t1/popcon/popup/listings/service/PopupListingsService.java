package com.t1.popcon.popup.listings.service;

import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.popup.dto.card.OverlayType;
import com.t1.popcon.popup.dto.card.PhaseStatus;
import com.t1.popcon.popup.dto.card.PhaseType;
import com.t1.popcon.popup.dto.card.PopupCardDto;
import com.t1.popcon.popup.dto.card.PopupSort;
import com.t1.popcon.popup.dto.section.PopupSectionResponse;
import com.t1.popcon.popup.dto.section.SectionKey;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Service
public class PopupListingsService {

    public PopupSectionResponse<PopupCardDto> getPopups(
        PhaseType phaseType,
        List<PhaseStatus> statuses,
        PopupSort sort,
        int limit
    ) {
        if (phaseType == null || statuses == null || statuses.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        if (limit < 1) {
            Map<String, String> errors = new LinkedHashMap<>();
            errors.put("limit", "limit는 1 이상이어야 합니다.");
            throw new CustomException(ErrorCode.INVALID_INPUT, errors);
        }

        List<PopupCardDto> items = Stream.of(
                createAuctionOpenMocks(),
                createAuctionUpcomingMocks(),
                createDrawOpenMocks(),
                createDrawUpcomingMocks(),
                createClosedMocks()
            )
            .flatMap(List::stream)
            .filter(popup -> popup.phase().type() == phaseType)
            .filter(popup -> statuses.contains(popup.phase().status()))
            .limit(limit)
            .toList();

        return new PopupSectionResponse<>(
            resolveSectionKey(phaseType, statuses),
            items.size(),
            items
        );
    }

    public String getMessage(PhaseType type, List<PhaseStatus> statuses) {
        if (type == null || statuses == null || statuses.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        if (type == PhaseType.AUCTION) {
            return "더치 경매 섹션 조회를 성공했습니다.";
        }

        if (statuses.size() == 1 && statuses.contains(PhaseStatus.OPEN)) {
            return "진행 중 드로우 섹션 조회를 성공했습니다.";
        }

        if (statuses.size() == 1 && statuses.contains(PhaseStatus.UPCOMING)) {
            return "오픈 예정 드로우 섹션 조회를 성공했습니다.";
        }

        return "드로우 섹션 조회를 성공했습니다.";
    }

    private SectionKey resolveSectionKey(PhaseType type, List<PhaseStatus> statuses) {
        if (type == PhaseType.AUCTION) {
            return SectionKey.AUCTIONS;
        }

        if (statuses.size() == 1 && statuses.contains(PhaseStatus.OPEN)) {
            return SectionKey.DRAWS_OPEN;
        }

        if (statuses.size() == 1 && statuses.contains(PhaseStatus.UPCOMING)) {
            return SectionKey.DRAWS_UPCOMING;
        }

        throw new CustomException(ErrorCode.INVALID_INPUT);
    }

    private List<PopupCardDto> createAuctionOpenMocks() {
        return IntStream.rangeClosed(1, 5)
            .mapToObj(i -> new PopupCardDto(
                300L + i,
                "진행 중 경매 팝업 " + i,
                null,
                "AUCTION SUBTEXT " + i,
                "진행 중 더치 경매 캡션",
                null,
                i % 2 == 0,
                new PopupCardDto.StatsDto(100 + i, 1000 + i * 100),
                new PopupCardDto.OverlayDto(OverlayType.AUCTION_IN_PROGRESS, null),
                new PopupCardDto.PhaseDto(
                    PhaseType.AUCTION,
                    PhaseStatus.OPEN,
                    OffsetDateTime.parse("2026-03-04T10:00:00+09:00").plusDays(i),
                    OffsetDateTime.parse("2026-03-04T10:30:00+09:00").plusDays(i)
                )
            ))
            .toList();
    }

    private List<PopupCardDto> createAuctionUpcomingMocks() {
        return IntStream.rangeClosed(1, 5)
            .mapToObj(i -> new PopupCardDto(
                400L + i,
                "오픈 예정 경매 팝업 " + i,
                null,
                "AUCTION SUBTEXT " + i,
                "오픈 예정 경매 캡션",
                null,
                i % 2 != 0,
                new PopupCardDto.StatsDto(80 + i, 900 + i * 90),
                new PopupCardDto.OverlayDto(OverlayType.AUCTION_OPEN_AT, null),
                new PopupCardDto.PhaseDto(
                    PhaseType.AUCTION,
                    PhaseStatus.UPCOMING,
                    OffsetDateTime.parse("2026-03-10T10:00:00+09:00").plusDays(i),
                    OffsetDateTime.parse("2026-03-10T10:30:00+09:00").plusDays(i)
                )
            ))
            .toList();
    }

    private List<PopupCardDto> createDrawOpenMocks() {
        return IntStream.rangeClosed(1, 10)
            .mapToObj(i -> new PopupCardDto(
                500L + i,
                "진행 중 드로우 팝업 " + i,
                null,
                "DRAW SUBTEXT " + i,
                "진행 중 드로우 캡션",
                null,
                i % 2 == 0,
                new PopupCardDto.StatsDto(60 + i, 700 + i * 80),
                null,
                new PopupCardDto.PhaseDto(
                    PhaseType.DRAW,
                    PhaseStatus.OPEN,
                    OffsetDateTime.parse("2026-02-23T10:30:00+09:00").plusDays(i),
                    OffsetDateTime.parse("2026-02-24T10:30:00+09:00").plusDays(i)
                )
            ))
            .toList();
    }

    private List<PopupCardDto> createDrawUpcomingMocks() {
        return IntStream.rangeClosed(1, 10)
            .mapToObj(i -> new PopupCardDto(
                600L + i,
                "오픈 예정 드로우 팝업 " + i,
                null,
                "DRAW SUBTEXT " + i,
                "오픈 예정 드로우 캡션",
                null,
                i % 2 != 0,
                new PopupCardDto.StatsDto(40 + i, 500 + i * 60),
                new PopupCardDto.OverlayDto(OverlayType.DRAW_OPEN_AT, null),
                new PopupCardDto.PhaseDto(
                    PhaseType.DRAW,
                    PhaseStatus.UPCOMING,
                    OffsetDateTime.parse("2026-03-01T10:30:00+09:00").plusDays(i),
                    OffsetDateTime.parse("2026-03-02T10:30:00+09:00").plusDays(i)
                )
            ))
            .toList();
    }

    private List<PopupCardDto> createClosedMocks() {
        return IntStream.rangeClosed(1, 5)
            .mapToObj(i -> new PopupCardDto(
                700L + i,
                "종료된 팝업 " + i,
                null,
                "CLOSED SUBTEXT " + i,
                "종료된 팝업",
                null,
                false,
                new PopupCardDto.StatsDto(20 + i, 300 + i * 30),
                null,
                new PopupCardDto.PhaseDto(
                    i % 2 == 0 ? PhaseType.AUCTION : PhaseType.DRAW,
                    PhaseStatus.CLOSED,
                    OffsetDateTime.parse("2026-02-10T10:00:00+09:00").plusDays(i),
                    OffsetDateTime.parse("2026-02-11T10:00:00+09:00").plusDays(i)
                )
            ))
            .toList();
    }
}