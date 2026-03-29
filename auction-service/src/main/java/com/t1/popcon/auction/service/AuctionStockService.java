package com.t1.popcon.auction.service;

import com.t1.popcon.auction.bid.infrastructure.BidRedisRepository;
import com.t1.popcon.auction.domain.AuctionOption;
import com.t1.popcon.auction.repository.AuctionOptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuctionStockService {

	private final AuctionOptionRepository auctionOptionRepository;
	private final BidRedisRepository bidRedisRepository;

	public boolean hasAvailableStock(Long auctionId) {
		return auctionOptionRepository.findByAuction_IdOrderByEntryDateAscEntryTimeAsc(auctionId)
			.stream()
			.anyMatch(option -> getAvailableStock(option) > 0);
	}

	public void releasePendingRestocks(Long auctionId) {
		auctionOptionRepository.findByAuction_IdOrderByEntryDateAscEntryTimeAsc(auctionId)
			.forEach(option -> bidRedisRepository.releasePendingRestock(option.getId()));
	}

	public Map<Long, OptionStockSnapshot> getOptionStocks(Long auctionId) {
		Map<Long, OptionStockSnapshot> stocks = new LinkedHashMap<>();

		for (AuctionOption option : auctionOptionRepository.findByAuction_IdOrderByEntryDateAscEntryTimeAsc(auctionId)) {
			stocks.put(option.getId(), new OptionStockSnapshot(
				option.getId(),
				option.getEntryDate(),
				option.getEntryTime(),
				getAvailableStock(option),
				getPendingRestock(option)
			));
		}

		return stocks;
	}

	public List<OptionStockSnapshot> getOptionStocksByDate(Long auctionId, LocalDate entryDate) {
		return auctionOptionRepository.findByAuction_IdAndEntryDateOrderByEntryTimeAsc(auctionId, entryDate)
			.stream()
			.map(option -> new OptionStockSnapshot(
				option.getId(),
				option.getEntryDate(),
				option.getEntryTime(),
				getAvailableStock(option),
				getPendingRestock(option)
			))
			.toList();
	}

	private int getAvailableStock(AuctionOption option) {
		Integer availableStock = bidRedisRepository.getAvailableStock(option.getId());
		if (availableStock != null) {
			return availableStock;
		}

		bidRedisRepository.setAvailableStock(option.getId(), option.getRemainingStock());
		return option.getRemainingStock();
	}

	private int getPendingRestock(AuctionOption option) {
		return bidRedisRepository.getPendingRestock(option.getId());
	}

	public record OptionStockSnapshot(
		Long optionId,
		LocalDate entryDate,
		LocalTime entryTime,
		int availableStock,
		int pendingStock
	) {
		public boolean hasStockForListing() {
			return availableStock > 0 || pendingStock > 0;
		}

		public boolean isSelectable() {
			return availableStock > 0;
		}
	}
}
