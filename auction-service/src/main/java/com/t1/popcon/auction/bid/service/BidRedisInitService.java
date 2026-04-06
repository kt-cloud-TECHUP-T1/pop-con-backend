package com.t1.popcon.auction.bid.service;

import com.t1.popcon.auction.domain.AuctionOption;
import com.t1.popcon.auction.repository.AuctionOptionRepository;
import com.t1.popcon.auction.service.AuctionStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BidRedisInitService {

	private final AuctionOptionRepository auctionOptionRepository;
	private final AuctionStockService auctionStockService;

	@Transactional(readOnly = true)
	public void initializeStockToRedis(Long auctionId) {
		log.info(">>>> [Redis Init] auctionId={} 재고 초기화 시작", auctionId);

		List<AuctionOption> options = auctionOptionRepository
			.findByAuction_IdOrderByEntryDateAscEntryTimeAsc(auctionId);

		if (options.isEmpty()) {
			log.warn(">>>> [Redis Init] 초기화할 재고 정보가 없습니다. auctionId={}", auctionId);
			return;
		}

		for (AuctionOption option : options) {
			auctionStockService.initializeOptionStock(option, false);
			log.info(">>>> [Redis Init] optionId={} availableStock 초기화 완료", option.getId());
		}

		log.info(">>>> [Redis Init] auctionId={} 재고 초기화 완료 (총 {}개 회차)", auctionId, options.size());
	}
}
