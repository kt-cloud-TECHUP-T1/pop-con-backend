package com.t1.popcon.user.service;

import com.t1.popcon.common.response.ApiResponse;
import com.t1.popcon.user.client.AuctionServiceClient;
import com.t1.popcon.user.client.DrawServiceClient;
import com.t1.popcon.user.dto.history.AuctionHistoryResponse;
import com.t1.popcon.user.dto.history.DrawHistoryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserHistoryService {

	private final DrawServiceClient drawServiceClient;
	private final AuctionServiceClient auctionServiceClient;

	public List<DrawHistoryResponse> getDrawHistory(Long userId) {
		try {
			ApiResponse<List<DrawHistoryResponse>> response = drawServiceClient.getDrawEntries(userId);
			if (response != null && response.getData() != null) {
				return response.getData();
			}
		} catch (Exception e) {
			log.error(">>>> [Draw-Service 연동 실패] User ID: {}, Error: {}", userId, e.getMessage());
		}
		return Collections.emptyList();
	}

	public List<AuctionHistoryResponse> getAuctionHistory(Long userId) {
		try {
			ApiResponse<List<AuctionHistoryResponse>> response = auctionServiceClient.getAuctionBids(userId);
			if (response != null && response.getData() != null) {
				return response.getData();
			}
		} catch (Exception e) {
			log.error(">>>> [Auction-Service 연동 실패] User ID: {}, Error: {}", userId, e.getMessage());
		}
		return Collections.emptyList();
	}
}
