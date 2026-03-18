package com.t1.popcon.auction.bid.service;

import com.t1.popcon.auction.bid.infrastructure.BidRedisRepository;
import com.t1.popcon.auction.domain.AuctionOption;
import com.t1.popcon.auction.repository.AuctionOptionRepository;
import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BidAdminService {

	private final AuctionOptionRepository auctionOptionRepository;
	private final BidRedisRepository bidRedisRepository;

	@Transactional(readOnly = true)
	public void initStockToRedis(Long optionId) {
		// 1. DB에서 해당 회차 조회
		AuctionOption option = auctionOptionRepository.findById(optionId)
			.orElseThrow(() -> new CustomException(ErrorCode.AUCTION_OPTION_NOT_FOUND));

		// 2. DB의 현재 남은 재고를 Redis에 세팅
		int stock = option.getRemainingStock();
		bidRedisRepository.setStock(optionId, stock);

		log.info(">>>> [Admin] Redis Stock Initialized. Option ID: {}, Stock: {}", optionId, stock);
	}
}