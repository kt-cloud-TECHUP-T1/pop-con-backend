package com.t1.popcon.user.service;

import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.user.client.AuctionServiceClient;
import com.t1.popcon.user.client.DrawServiceClient;
import com.t1.popcon.user.client.PopupServiceClient;
import com.t1.popcon.user.client.TicketServiceClient;
import com.t1.popcon.user.dto.history.AuctionHistoryResponse;
import com.t1.popcon.user.dto.history.AuctionReservationDetailResponse;
import com.t1.popcon.user.dto.history.DrawEntryDetailResponse;
import com.t1.popcon.user.dto.history.DrawHistoryResponse;
import com.t1.popcon.user.dto.history.PopupInternalResponse;
import com.t1.popcon.user.dto.history.PopupLikeHistoryResponse;
import com.t1.popcon.user.dto.history.SliceResponse;
import com.t1.popcon.user.dto.history.TicketDetailResponse;
import com.t1.popcon.user.dto.history.TicketDetailViewResponse;
import com.t1.popcon.user.dto.history.TicketHistoryResponse;
import com.t1.popcon.user.dto.history.TicketPurchaserProfileResponse;
import com.t1.popcon.user.dto.statistics.AuctionStatisticsInternalResponse;
import com.t1.popcon.user.dto.statistics.DrawStatisticsInternalResponse;
import com.t1.popcon.user.dto.statistics.UserActivityStatisticsResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserHistoryService {

    private static final int DEFAULT_HISTORY_PAGE = 0;
    private static final int DEFAULT_HISTORY_SIZE = 100;

    private final DrawServiceClient drawServiceClient;
    private final AuctionServiceClient auctionServiceClient;
    private final TicketServiceClient ticketServiceClient;
    private final PopupServiceClient popupServiceClient;
    private final UserService userService;

    public UserActivityStatisticsResponse getMyStatistics(Long userId) {
        CompletableFuture<DrawStatisticsInternalResponse> drawFuture = CompletableFuture.supplyAsync(() -> {
            try {
                ApiResponse<DrawStatisticsInternalResponse> response = drawServiceClient.getDrawStatistics(userId);
                return (response != null && response.getData() != null) ? response.getData() : null;
            } catch (Exception e) {
                log.error("Failed to fetch draw statistics for user: {}. Error: {}", userId, e.getMessage());
                return null;
            }
        });

        CompletableFuture<AuctionStatisticsInternalResponse> auctionFuture = CompletableFuture.supplyAsync(() -> {
            try {
                ApiResponse<AuctionStatisticsInternalResponse> response = auctionServiceClient.getAuctionStatistics(userId);
                return (response != null && response.getData() != null) ? response.getData() : null;
            } catch (Exception e) {
                log.error("Failed to fetch auction statistics for user: {}. Error: {}", userId, e.getMessage());
                return null;
            }
        });

        CompletableFuture<Long> ticketCountFuture = CompletableFuture.supplyAsync(() -> {
            try {
                ApiResponse<Long> response = ticketServiceClient.countTickets(userId);
                return (response != null && response.getData() != null) ? response.getData() : 0L;
            } catch (Exception e) {
                log.error("Failed to fetch ticket count for user: {}. Error: {}", userId, e.getMessage());
                return 0L;
            }
        });

        CompletableFuture<Long> likedPopupCountFuture = CompletableFuture.supplyAsync(() -> {
            try {
                ApiResponse<Long> response = popupServiceClient.countLikedPopups(userId);
                return (response != null && response.getData() != null) ? response.getData() : 0L;
            } catch (Exception e) {
                log.error("Failed to fetch liked popup count for user: {}. Error: {}", userId, e.getMessage());
                return 0L;
            }
        });

        CompletableFuture.allOf(drawFuture, auctionFuture, ticketCountFuture, likedPopupCountFuture).join();

        DrawStatisticsInternalResponse drawStats = drawFuture.join();
        AuctionStatisticsInternalResponse auctionStats = auctionFuture.join();
        long ticketCount = ticketCountFuture.join();
        long likedPopupCount = likedPopupCountFuture.join();

        return new UserActivityStatisticsResponse(
            ticketCount,
            drawStats != null ? drawStats.totalCount() : 0,
            auctionStats != null ? auctionStats.totalCount() : 0,
            0L, // reviewCount (placeholder)
            drawStats != null ? drawStats.wonCount() : 0,
            drawStats != null ? drawStats.lostCount() : 0,
            drawStats != null ? drawStats.ongoingCount() : 0,
            drawStats != null ? drawStats.waitingResultCount() : 0,
            auctionStats != null ? auctionStats.wonCount() : 0,
            likedPopupCount
        );
    }

    public List<DrawHistoryResponse> getDrawHistory(Long userId) {
        try {
            List<DrawHistoryResponse> histories = new ArrayList<>();
            int page = DEFAULT_HISTORY_PAGE;

            while (true) {
                ApiResponse<SliceResponse<DrawHistoryResponse>> response = drawServiceClient.getDrawEntries(
                    userId,
                    page,
                    DEFAULT_HISTORY_SIZE
                );
                if (response == null || response.getData() == null || response.getData().getContent() == null) {
                    throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
                }

                List<DrawHistoryResponse> content = response.getData().getContent();
                if (content.isEmpty()) {
                    break;
                }

                histories.addAll(content);
                if (response.getData().isLast() || content.size() < DEFAULT_HISTORY_SIZE) {
                    break;
                }

                page++;
            }

            return histories;
        } catch (Exception e) {
            log.error("Draw service integration failed. userId={}, error={}", userId, e.getMessage());
            throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
        }
    }

    public List<AuctionHistoryResponse> getAuctionHistory(Long userId) {
        try {
            ApiResponse<List<AuctionHistoryResponse>> response = auctionServiceClient.getAuctionBids(userId);
            if (response == null || response.getData() == null) {
                throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
            }
            return response.getData();
        } catch (Exception e) {
            log.error("Auction service integration failed. userId={}, error={}", userId, e.getMessage());
            throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
        }
    }

    public SliceResponse<TicketHistoryResponse> getTicketHistory(Long userId, int page, int size) {
        try {
            ApiResponse<SliceResponse<TicketHistoryResponse>> response = ticketServiceClient.getTickets(userId, page, size);
            if (response == null || response.getData() == null) {
                throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
            }

            SliceResponse<TicketHistoryResponse> slice = response.getData();
            List<TicketHistoryResponse> content = slice.getContent() == null ? Collections.emptyList() : slice.getContent();
            Map<Long, PopupInternalResponse> popupMap = fetchPopupsInBatch(extractPopupIds(content));

            List<TicketHistoryResponse> enrichedTickets = content.stream()
                .map(ticket -> enrichTicketSummary(ticket, popupMap.get(ticket.getPopupId())))
                .toList();

            return new SliceResponse<>(
                enrichedTickets,
                slice.isFirst(),
                slice.isLast(),
                slice.getNumberOfElements(),
                slice.isEmpty()
            );
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ticket service integration failed. userId={}, error={}", userId, e.getMessage());
            throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
        }
    }

    public TicketDetailViewResponse getTicketById(Long userId, Long ticketId) {
        try {
            ApiResponse<TicketDetailResponse> response = ticketServiceClient.getTicketById(ticketId, userId);
            if (response == null || response.getData() == null) {
                throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
            }

            TicketDetailResponse ticket = response.getData();
            TicketPurchaserProfileResponse purchaser = userService.getTicketPurchaserProfile(userId);

            TicketDetailViewResponse.TicketDetailViewResponseBuilder detailBuilder = TicketDetailViewResponse.builder()
                .ticketId(ticket.getTicketId())
                .ticketNumber(ticket.getTicketNumber())
                .reservationNo(ticket.getReservationNo())
                .status(ticket.getStatus())
                .displayStatus(resolveDisplayStatus(null, ticket.getStatus()))
                .sourceType(ticket.getSourceType())
                .sourceId(ticket.getSourceId())
                .popupId(ticket.getPopupId())
                .entryDate(ticket.getEntryDate())
                .entryTime(ticket.getEntryTime())
                .issuedAt(ticket.getIssuedAt())
                .qrValue(resolveQrValue(ticket))
                .userName(purchaser.userName())
                .userPhoneNumber(purchaser.userPhoneNumber())
                .userEmail(purchaser.userEmail());

            applySourceSpecificDetail(detailBuilder, userId, ticket);
            applyPopupFallback(detailBuilder, ticket.getPopupId());
            return detailBuilder.build();
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ticket detail lookup failed. userId={}, ticketId={}, error={}", userId, ticketId, e.getMessage());
            throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
        }
    }

    public TicketHistoryResponse getTicketByReservationNo(Long userId, String reservationNo) {
        try {
            ApiResponse<TicketDetailResponse> response = ticketServiceClient.getTicketByReservationNo(reservationNo, userId);
            if (response == null || response.getData() == null) {
                throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
            }

            TicketDetailResponse ticket = response.getData();
            PopupInternalResponse popup = fetchPopupSafely(ticket.getPopupId());
            return toTicketHistoryResponse(ticket, popup);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ticket service integration failed. userId={}, reservationNo={}, error={}",
                userId, reservationNo, e.getMessage());
            throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
        }
    }

    public SliceResponse<PopupLikeHistoryResponse> getLikedPopups(Long userId, int page, int size) {
        try {
            ApiResponse<SliceResponse<PopupLikeHistoryResponse>> response =
                popupServiceClient.getLikedPopups(userId, page, size);
            if (response == null || response.getData() == null) {
                throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
            }
            return response.getData();
        } catch (Exception e) {
            log.error("Popup service integration failed. userId={}, error={}", userId, e.getMessage());
            throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
        }
    }

    private TicketHistoryResponse enrichTicketSummary(TicketHistoryResponse ticket, PopupInternalResponse popup) {
        return TicketHistoryResponse.builder()
            .ticketId(ticket.getTicketId())
            .userId(ticket.getUserId())
            .popupId(ticket.getPopupId())
            .ticketNumber(ticket.getTicketNumber())
            .reservationNo(ticket.getReservationNo())
            .status(ticket.getStatus())
            .sourceType(ticket.getSourceType())
            .sourceId(ticket.getSourceId())
            .entryDate(ticket.getEntryDate())
            .entryTime(ticket.getEntryTime())
            .issuedAt(ticket.getIssuedAt())
            .popupTitle(firstNonBlank(popup != null ? popup.title() : null, ticket.getPopupTitle()))
            .popupAddress(firstNonBlank(popup != null ? popup.location() : null, ticket.getPopupAddress()))
            .thumbnailUrl(firstNonBlank(popup != null ? popup.vThumbnailUrl() : null, ticket.getThumbnailUrl()))
            .displayStatus(resolveDisplayStatus(ticket.getDisplayStatus(), ticket.getStatus()))
            .build();
    }

    private TicketHistoryResponse toTicketHistoryResponse(TicketDetailResponse ticket, PopupInternalResponse popup) {
        return TicketHistoryResponse.builder()
            .ticketId(ticket.getTicketId())
            .userId(ticket.getUserId())
            .popupId(ticket.getPopupId())
            .ticketNumber(ticket.getTicketNumber())
            .reservationNo(ticket.getReservationNo())
            .status(ticket.getStatus())
            .sourceType(ticket.getSourceType())
            .sourceId(ticket.getSourceId())
            .entryDate(ticket.getEntryDate())
            .entryTime(ticket.getEntryTime())
            .issuedAt(ticket.getIssuedAt())
            .popupTitle(popup != null ? popup.title() : null)
            .popupAddress(popup != null ? popup.location() : null)
            .thumbnailUrl(popup != null ? popup.vThumbnailUrl() : null)
            .displayStatus(resolveDisplayStatus(null, ticket.getStatus()))
            .build();
    }

    private void applySourceSpecificDetail(
        TicketDetailViewResponse.TicketDetailViewResponseBuilder detailBuilder,
        Long userId,
        TicketDetailResponse ticket
    ) {
        String sourceType = ticket.getSourceType();
        if (sourceType == null) {
            return;
        }

        if ("AUCTION".equalsIgnoreCase(sourceType)) {
            TicketDetailViewResponse currentDetail = detailBuilder.build();
            if (ticket.getSourceId() == null) {
                log.warn("Auction ticket is missing sourceId. ticketId={}, sourceType={}",
                    ticket.getTicketId(), ticket.getSourceType());
                return;
            }
            ApiResponse<AuctionReservationDetailResponse> response =
                auctionServiceClient.getBidDetail(ticket.getSourceId(), userId);
            AuctionReservationDetailResponse detail = requireData(response);

            detailBuilder
                .popupTitle(firstNonBlank(detail.getPopupTitle(), currentDetail.getPopupTitle()))
                .popupAddress(firstNonBlank(detail.getPopupAddress(), currentDetail.getPopupAddress()))
                .thumbnailUrl(firstNonBlank(detail.getPopupThumbnail(), currentDetail.getThumbnailUrl()))
                .paidAt(detail.getPaidAt())
                .originalPrice(detail.getStartPrice())
                .discountAmount(detail.getDiscountAmount())
                .finalPrice(detail.getFinalPrice());
            return;
        }

        if ("DRAW".equalsIgnoreCase(sourceType)) {
            TicketDetailViewResponse currentDetail = detailBuilder.build();
            if (ticket.getSourceId() == null) {
                log.warn("Draw ticket is missing sourceId. ticketId={}, sourceType={}",
                    ticket.getTicketId(), ticket.getSourceType());
                return;
            }
            ApiResponse<DrawEntryDetailResponse> response =
                drawServiceClient.getDrawEntryDetail(ticket.getSourceId(), userId);
            DrawEntryDetailResponse detail = requireData(response);

            detailBuilder
                .popupTitle(firstNonBlank(detail.getPopupTitle(), currentDetail.getPopupTitle()))
                .popupAddress(firstNonBlank(detail.getPopupAddress(), currentDetail.getPopupAddress()))
                .thumbnailUrl(firstNonBlank(detail.getPopupThumbnail(), currentDetail.getThumbnailUrl()))
                .paidAt(detail.getPaidAt())
                .userName(firstNonBlank(detail.getUserName(), currentDetail.getUserName()))
                .userPhoneNumber(firstNonBlank(detail.getUserPhoneNumber(), currentDetail.getUserPhoneNumber()));
        }
    }

    private void applyPopupFallback(TicketDetailViewResponse.TicketDetailViewResponseBuilder detailBuilder, Long popupId) {
        TicketDetailViewResponse currentDetail = detailBuilder.build();
        if (!needsPopupFallback(currentDetail)) {
            return;
        }

        PopupInternalResponse popup = fetchPopupSafely(popupId);
        if (popup == null) {
            return;
        }

        detailBuilder
            .popupTitle(firstNonBlank(currentDetail.getPopupTitle(), popup.title()))
            .popupAddress(firstNonBlank(currentDetail.getPopupAddress(), popup.location()))
            .thumbnailUrl(firstNonBlank(currentDetail.getThumbnailUrl(), popup.vThumbnailUrl()));
    }

    private boolean needsPopupFallback(TicketDetailViewResponse detail) {
        return isBlank(detail.getPopupTitle()) || isBlank(detail.getPopupAddress()) || isBlank(detail.getThumbnailUrl());
    }

    private PopupInternalResponse fetchPopup(Long popupId) {
        if (popupId == null || popupId <= 0) {
            return null;
        }

        ApiResponse<PopupInternalResponse> response = popupServiceClient.getPopupDetail(popupId);
        return response != null ? response.getData() : null;
    }

    private PopupInternalResponse fetchPopupSafely(Long popupId) {
        try {
            return fetchPopup(popupId);
        } catch (Exception e) {
            log.warn("Popup fallback fetch failed. popupId={}, error={}", popupId, e.getMessage());
            return null;
        }
    }

    private Map<Long, PopupInternalResponse> fetchPopupsInBatch(List<Long> popupIds) {
        if (popupIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            ApiResponse<List<PopupInternalResponse>> response = popupServiceClient.getPopupsByBulkIds(popupIds);
            List<PopupInternalResponse> popups =
                response != null && response.getData() != null ? response.getData() : Collections.emptyList();

            return popups.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(PopupInternalResponse::popupId, Function.identity(), (left, right) -> left));
        } catch (Exception e) {
            log.warn("Popup bulk fetch failed. popupIds={}, error={}", popupIds, e.getMessage());
            return Collections.emptyMap();
        }
    }

    private List<Long> extractPopupIds(List<TicketHistoryResponse> tickets) {
        return tickets.stream()
            .map(TicketHistoryResponse::getPopupId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }

    private String resolveDisplayStatus(String displayStatus, String status) {
        if (!isBlank(displayStatus)) {
            return displayStatus;
        }
        if (isBlank(status)) {
            return null;
        }

        return switch (status.toUpperCase()) {
            case "ISSUED" -> "사용 가능";
            case "USED" -> "사용 완료";
            case "CANCELLED" -> "취소됨";
            default -> status;
        };
    }

    private String resolveQrValue(TicketDetailResponse ticket) {
        if (ticket.getTicketNumber() != null && !ticket.getTicketNumber().isBlank()) {
            return ticket.getTicketNumber();
        }
        return ticket.getReservationNo();
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private <T> T requireData(ApiResponse<T> response) {
        if (response == null || response.getData() == null) {
            throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
        }
        return response.getData();
    }
}
