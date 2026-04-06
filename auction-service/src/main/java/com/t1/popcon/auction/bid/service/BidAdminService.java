package com.t1.popcon.auction.bid.service;

import com.t1.popcon.auction.domain.AuctionOption;
import com.t1.popcon.auction.repository.AuctionOptionRepository;
import com.t1.popcon.auction.service.AuctionStockService;
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
	private final AuctionStockService auctionStockService;

	@Transactional(readOnly = true)
	public void initStockToRedis(Long optionId) {
		AuctionOption option = auctionOptionRepository.findById(optionId)
			.orElseThrow(() -> new CustomException(ErrorCode.AUCTION_OPTION_NOT_FOUND));

		auctionStockService.initializeOptionStock(option, true);

		log.info(">>>> [Admin] optionId={} availableStock 재구성 완료 (pendingRestock 보존)", optionId);
	}
}
