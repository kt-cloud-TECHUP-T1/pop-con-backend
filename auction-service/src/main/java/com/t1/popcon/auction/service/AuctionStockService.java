package com.t1.popcon.auction.service;

import com.t1.popcon.auction.bid.infrastructure.BidRedisRepository;
import com.t1.popcon.auction.domain.AuctionOption;
import com.t1.popcon.auction.repository.AuctionOptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
		boolean hasAvailableStock = false;

		for (AuctionOption option : auctionOptionRepository.findByAuction_IdOrderByEntryDateAscEntryTimeAsc(auctionId)) {
			if (getAvailableStock(option) > 0) {
				hasAvailableStock = true;
			}
		}

		return hasAvailableStock;
	}

	public long releasePendingRestocks(Long auctionId) {
		long released = 0L;

		for (AuctionOption option : auctionOptionRepository.findByAuction_IdOrderByEntryDateAscEntryTimeAsc(auctionId)) {
			released += bidRedisRepository.releasePendingRestock(option.getId());
		}

		return released;
	}

	public void initializeOptionStock(AuctionOption option, boolean preservePendingRestock) {
		int pendingRestock = preservePendingRestock ? bidRedisRepository.getPendingRestock(option.getId()) : 0;
		int availableStock = Math.max(option.getRemainingStock() - pendingRestock, 0);

		bidRedisRepository.setAvailableStock(option.getId(), availableStock);
	}

	public void recordSoldOut(Long auctionId, Integer soldOutPrice) {
		bidRedisRepository.setSoldOutPrice(auctionId, soldOutPrice);
		bidRedisRepository.clearRestockAnchorAt(auctionId);
	}

	public boolean recordSoldOutIfAbsent(Long auctionId, Integer soldOutPrice) {
		boolean stored = bidRedisRepository.setSoldOutPriceIfAbsent(auctionId, soldOutPrice);
		if (stored) {
			bidRedisRepository.clearRestockAnchorAt(auctionId);
		}
		return stored;
	}

	public void recordRestockAnchor(Long auctionId, LocalDateTime restockAnchorAt) {
		bidRedisRepository.setRestockAnchorAt(auctionId, restockAnchorAt);
	}

	public PriceAnchor getPriceAnchor(Long auctionId) {
		return new PriceAnchor(
			bidRedisRepository.getSoldOutPrice(auctionId),
			bidRedisRepository.getRestockAnchorAt(auctionId)
		);
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

		initializeOptionStock(option, true);
		return Math.max(option.getRemainingStock() - bidRedisRepository.getPendingRestock(option.getId()), 0);
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

	public record PriceAnchor(
		Integer soldOutPrice,
		LocalDateTime restockAnchorAt
	) {
	}
}
