package com.t1.popcon.user.service;

import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.user.client.AuctionServiceClient;
import com.t1.popcon.user.client.DrawServiceClient;
import com.t1.popcon.user.client.PopupServiceClient;
import com.t1.popcon.user.client.TicketServiceClient;
import com.t1.popcon.user.dto.history.AuctionHistoryResponse;
import com.t1.popcon.user.dto.history.DrawHistoryResponse;
import com.t1.popcon.user.dto.history.PopupLikeHistoryResponse;
import com.t1.popcon.user.dto.history.SliceResponse;
import com.t1.popcon.user.dto.history.TicketHistoryResponse;
import java.util.ArrayList;
import java.util.List;
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
            log.error(">>>> [Draw-Service 연동 실패] User ID: {}, Error: {}", userId, e.getMessage());
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
            log.error(">>>> [Auction-Service 연동 실패] User ID: {}, Error: {}", userId, e.getMessage());
            throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
        }
    }

    public SliceResponse<TicketHistoryResponse> getTicketHistory(Long userId, int page, int size) {
        try {
            ApiResponse<SliceResponse<TicketHistoryResponse>> response = ticketServiceClient.getTickets(userId, page, size);
            if (response == null || response.getData() == null) {
                throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
            }
            return response.getData();
        } catch (Exception e) {
            log.error(">>>> [Ticket-Service 연동 실패] User ID: {}, Error: {}", userId, e.getMessage());
            throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
        }
    }

    public TicketHistoryResponse getTicketByReservationNo(Long userId, String reservationNo) {
        try {
            ApiResponse<TicketHistoryResponse> response = ticketServiceClient.getTicketByReservationNo(reservationNo, userId);
            if (response == null || response.getData() == null) {
                throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
            }
            return response.getData();
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error(">>>> [Ticket-Service 연동 실패] userId={}, reservationNo={}, Error: {}", userId, reservationNo, e.getMessage());
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
            log.error(">>>> [Popup-Service 연동 실패] User ID: {}, Error: {}", userId, e.getMessage());
            throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
        }
    }
}
