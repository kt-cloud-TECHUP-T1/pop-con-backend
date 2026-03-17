package com.t1.popcon.auction.bid.service;

import com.t1.popcon.auction.bid.dto.BidRequest;
import com.t1.popcon.auction.bid.infrastructure.BidRedisRepository;
import com.t1.popcon.auction.domain.Auction;
import com.t1.popcon.auction.domain.AuctionOption;
import com.t1.popcon.auction.domain.AuctionStatus;
import com.t1.popcon.auction.repository.AuctionOptionRepository;
import com.t1.popcon.auction.repository.AuctionRepository;
import com.t1.popcon.common.infrastructure.portone.PortoneClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;

@SpringBootTest
@ActiveProfiles("test")
class BidConcurrencyTest {

	@Autowired private BidService bidService;
	@Autowired private BidRedisRepository bidRedisRepository;
	@Autowired private AuctionRepository auctionRepository;
	@Autowired private AuctionOptionRepository auctionOptionRepository;

	@MockitoBean
	private PortoneClient portoneClient;

	private Long savedOptionId;

	@BeforeEach
	void setUp() {
		Auction auction = auctionRepository.save(Auction.builder()
			.startPrice(10000)
			.minimumPrice(5000)
			.priceDropUnit(1000)
			.priceDropIntervalSeconds(3600)
			.stockPerOption(100)
			.openedAt(LocalDateTime.now().minusMinutes(1))
			.closedAt(LocalDateTime.now().plusHours(1))
			.status(AuctionStatus.OPEN)
			.build());

		AuctionOption option = auctionOptionRepository.save(AuctionOption.builder()
			.auction(auction)
			.entryDate(LocalDate.now())
			.entryTime(LocalTime.now())
			.remainingStock(10)
			.build());

		this.savedOptionId = option.getId();

		bidRedisRepository.setStock(savedOptionId, 10);
	}

	@Test
	@DisplayName("100명이 동시에 낙찰을 시도하면 재고만큼만 성공해야 한다")
	void concurrency_test() throws InterruptedException {
		doNothing().when(portoneClient).executePayment(any(), any(), anyInt(), any());

		int threadCount = 100;
		ExecutorService executorService = Executors.newFixedThreadPool(32);
		CountDownLatch latch = new CountDownLatch(threadCount);

		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger failCount = new AtomicInteger();

		for (int i = 0; i < threadCount; i++) {
			long userId = i + 1;
			executorService.submit(() -> {
				try {
					bidService.attemptBid(userId, new BidRequest(savedOptionId, 10000));
					successCount.incrementAndGet();
				} catch (Exception e) {
					System.err.println("낙찰 실패 원인: " + e.getMessage());
					e.printStackTrace();
					failCount.incrementAndGet();
				} finally {
					latch.countDown();
				}
			});
		}

		boolean completed = latch.await(30, TimeUnit.SECONDS);
		executorService.shutdown();
		assertThat(completed).isTrue();

		// [검증]
		System.out.println("Final Success Count: " + successCount.get());
		System.out.println("Final Fail Count: " + failCount.get());

		assertThat(successCount.get()).isEqualTo(10);
		assertThat(bidRedisRepository.getStock(savedOptionId)).isEqualTo(0);
	}
}