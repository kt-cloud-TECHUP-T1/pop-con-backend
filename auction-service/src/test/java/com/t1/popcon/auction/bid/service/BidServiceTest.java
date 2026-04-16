package com.t1.popcon.auction.bid.service;

import com.t1.popcon.auction.bid.client.PopupServiceClient;
import com.t1.popcon.auction.bid.client.TicketServiceClient;
import com.t1.popcon.auction.bid.client.UserBillingClient;
import com.t1.popcon.auction.bid.client.dto.BillingKeyInternalResponse;
import com.t1.popcon.auction.bid.client.dto.PopupInternalResponse;
import com.t1.popcon.auction.bid.client.dto.TicketIssueResponse;
import com.t1.popcon.auction.bid.domain.Bid;
import com.t1.popcon.auction.bid.domain.BidStatus;
import com.t1.popcon.auction.bid.dto.BidRequest;
import com.t1.popcon.auction.bid.dto.BidResponse;
import com.t1.popcon.auction.bid.infrastructure.BidRedisRepository;
import com.t1.popcon.auction.bid.repository.BidRepository;
import com.t1.popcon.auction.domain.Auction;
import com.t1.popcon.auction.domain.AuctionOption;
import com.t1.popcon.auction.domain.AuctionStatus;
import com.t1.popcon.auction.repository.AuctionOptionRepository;
import com.t1.popcon.auction.service.AuctionPriceService;
import com.t1.popcon.auction.service.AuctionStockService;
import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.common.infrastructure.dto.PortOneCancelResponse;
import com.t1.popcon.common.infrastructure.dto.PortOnePaymentResponse;
import com.t1.popcon.common.infrastructure.portone.PortOneClient;
import com.t1.popcon.common.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BidServiceTest {

	@Mock
	private BidRedisRepository bidRedisRepository;
	@Mock
	private BidRepository bidRepository;
	@Mock
	private AuctionOptionRepository auctionOptionRepository;
	@Mock
	private AuctionPriceService auctionPriceService;
	@Mock
	private AuctionStockService auctionStockService;
	@Mock
	private PortOneClient portOneClient;
	@Mock
	private UserBillingClient userBillingClient;
	@Mock
	private PopupServiceClient popupServiceClient;
	@Mock
	private TicketServiceClient ticketServiceClient;
	@Mock
	private BidTransactionManager txManager;
	@Mock
	private ReservationNoGenerator reservationNoGenerator;
	@Mock
	private RedissonClient redissonClient;
	@Mock
	private RLock lock;

	@Spy
	private io.micrometer.core.instrument.MeterRegistry registry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry();

	@Spy
	private io.micrometer.observation.ObservationRegistry observationRegistry = io.micrometer.observation.ObservationRegistry.create();

	@InjectMocks
	private BidService bidService;

	private final Long userId = 1L;
	private final Long optionId = 100L;
	private final Integer bidPrice = 50000;
	private Auction auction;
	private AuctionOption option;

	@BeforeEach
	void setUp() throws InterruptedException {
		auction = mock(Auction.class);
		option = mock(AuctionOption.class);
		
		lenient().when(option.getId()).thenReturn(optionId);
		lenient().when(option.getAuction()).thenReturn(auction);
		lenient().when(auction.getId()).thenReturn(1L);

		// Redisson Lock 모킹: 항상 락 획득 성공으로 설정
		lenient().when(redissonClient.getLock(anyString())).thenReturn(lock);
		lenient().when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
	}

	@Test
	@DisplayName("낙찰 시도 성공 - 모든 과정 정상")
	void attemptBid_Success() {
		// given
		BidRequest request = new BidRequest(optionId, bidPrice);
		when(auctionOptionRepository.findByIdWithAuction(optionId)).thenReturn(Optional.of(option));
		when(bidRepository.existsByUserIdAndAuctionIdAndStatus(anyLong(), anyLong(), any())).thenReturn(false);
		when(auctionStockService.hasAvailableStock(anyLong())).thenReturn(true);
		when(auctionStockService.getPriceAnchor(anyLong())).thenReturn(new AuctionStockService.PriceAnchor(null, null));
		when(auctionPriceService.calculateStatus(any(), any(), anyBoolean())).thenReturn(AuctionStatus.OPEN);
		when(auctionPriceService.calculateCurrentPrice(any(), any(), any(), any(), any())).thenReturn(bidPrice);
		when(bidRedisRepository.decrementStock(optionId)).thenReturn(5L);

		String merchantUid = "merchant_123";
		Bid bid = Bid.builder()
			.userId(userId)
			.auctionOption(option)
			.bidPrice(bidPrice)
			.merchantUid(merchantUid)
			.build();
		ReflectionTestUtils.setField(bid, "id", 1L);
		when(txManager.preparePendingBid(anyLong(), any(), anyInt(), anyString())).thenReturn(bid);

		BillingKeyInternalResponse billingKey = new BillingKeyInternalResponse("billing_key_abc");
		when(userBillingClient.getDefaultBillingKey(userId)).thenReturn(ApiResponse.ok(billingKey));

		PortOnePaymentResponse.Payment paymentDetail = new PortOnePaymentResponse.Payment("pg_tx_123", "2023-11-20T15:30:00Z");
		PortOnePaymentResponse paymentResponse = new PortOnePaymentResponse(paymentDetail);
		when(portOneClient.executePayment(anyString(), anyString(), anyInt(), anyString())).thenReturn(paymentResponse);

		PopupInternalResponse popupInfo = new PopupInternalResponse(1L, "Pop-up Title", "Location", "h_thumbnail.url", "v_thumbnail.url");
		when(auction.getPopupId()).thenReturn(1L);
		when(popupServiceClient.getPopupDetail(anyLong())).thenReturn(ApiResponse.ok(popupInfo));
		when(option.getEntryDate()).thenReturn(LocalDate.now());
		when(option.getEntryTime()).thenReturn(LocalTime.now());
		when(auction.getStartPrice()).thenReturn(10000);
		when(reservationNoGenerator.generate()).thenReturn("TKT123456789012");
		when(txManager.completeBidSuccess(anyLong(), anyLong(), anyString(), any(), anyString(), anyString(), anyString(), anyString(), any(), any(), anyInt()))
			.thenReturn("TKT123456789012");
		when(ticketServiceClient.issueTicket(any())).thenReturn(
			ApiResponse.ok(new TicketIssueResponse(1L, "TKT00000001", "TKT123456789012", "ISSUED", "AUCTION", 1L))
		);

		// when
		BidResponse response = bidService.attemptBid(userId, request);

		// then
		assertThat(response.status()).isEqualTo(BidStatus.SUCCESS);
		verify(txManager).completeBidSuccess(
			eq(bid.getId()), eq(optionId), eq("pg_tx_123"), any(LocalDateTime.class),
			eq("TKT123456789012"), eq("Pop-up Title"), eq("Location"), eq("v_thumbnail.url"),
			any(LocalDate.class), any(LocalTime.class), eq(10000)
		);
	}

	@Test
	@DisplayName("낙찰 시도 실패 - 재고 없음")
	void attemptBid_Fail_SoldOut() {
		// given
		BidRequest request = new BidRequest(optionId, bidPrice);
		when(auctionOptionRepository.findByIdWithAuction(optionId)).thenReturn(Optional.of(option));
		when(bidRepository.existsByUserIdAndAuctionIdAndStatus(anyLong(), anyLong(), any())).thenReturn(false);
		when(auctionStockService.hasAvailableStock(anyLong())).thenReturn(true);
		when(auctionStockService.getPriceAnchor(anyLong())).thenReturn(new AuctionStockService.PriceAnchor(null, null));
		when(auctionPriceService.calculateStatus(any(), any(), anyBoolean())).thenReturn(AuctionStatus.OPEN);
		when(auctionPriceService.calculateCurrentPrice(any(), any(), any(), any(), any())).thenReturn(bidPrice);
		when(bidRedisRepository.decrementStock(optionId)).thenReturn(-1L);

		// when & then
		assertThatThrownBy(() -> bidService.attemptBid(userId, request))
			.isInstanceOf(CustomException.class)
			.hasMessageContaining(ErrorCode.AUCTION_OPTION_SOLD_OUT.getMessage());
	}

	@Test
	@DisplayName("낙찰 시도 실패 - 결제 요청 오류 시 결제 취소 및 재고 복구")
	void attemptBid_Fail_PaymentError() {
		// given
		BidRequest request = new BidRequest(optionId, bidPrice);
		when(auctionOptionRepository.findByIdWithAuction(optionId)).thenReturn(Optional.of(option));
		when(bidRepository.existsByUserIdAndAuctionIdAndStatus(anyLong(), anyLong(), any())).thenReturn(false);
		when(auctionStockService.hasAvailableStock(anyLong())).thenReturn(true);
		when(auctionStockService.getPriceAnchor(anyLong())).thenReturn(new AuctionStockService.PriceAnchor(null, null));
		when(auctionPriceService.calculateStatus(any(), any(), anyBoolean())).thenReturn(AuctionStatus.OPEN);
		when(auctionPriceService.calculateCurrentPrice(any(), any(), any(), any(), any())).thenReturn(bidPrice);
		when(bidRedisRepository.decrementStock(optionId)).thenReturn(5L);

		Bid bid = Bid.builder()
			.userId(userId)
			.auctionOption(option)
			.bidPrice(bidPrice)
			.merchantUid("merchant_123")
			.build();
		ReflectionTestUtils.setField(bid, "id", 1L);
		when(txManager.preparePendingBid(anyLong(), any(), anyInt(), anyString())).thenReturn(bid);

		BillingKeyInternalResponse billingKey = new BillingKeyInternalResponse("billing_key_abc");
		when(userBillingClient.getDefaultBillingKey(userId)).thenReturn(ApiResponse.ok(billingKey));

		when(portOneClient.executePayment(anyString(), anyString(), anyInt(), anyString()))
			.thenThrow(new RuntimeException("Payment API Error"));
		when(portOneClient.cancelPayment(anyString(), anyInt()))
			.thenReturn(new PortOneCancelResponse(
				new PortOneCancelResponse.Cancellation("SUCCEEDED", "cancel_1", "pg_cancel_1", bidPrice, "test", "2026-03-30T00:00:00Z")
			));

		// when & then
		assertThatThrownBy(() -> bidService.attemptBid(userId, request))
			.isInstanceOf(CustomException.class);

		verify(portOneClient).cancelPayment(anyString(), anyInt());
		verify(bidRedisRepository).addPendingRestock(optionId, 1L);
		verify(txManager).completeBidFailure(any());
	}

	@Test
	@DisplayName("낙찰 시도 실패 - 이미 해당 경매에 낙찰된 내역이 있음")
	void attemptBid_Fail_AlreadyParticipated() {
		// given
		BidRequest request = new BidRequest(optionId, bidPrice);
		when(auctionOptionRepository.findByIdWithAuction(optionId)).thenReturn(Optional.of(option));
		when(bidRepository.existsByUserIdAndAuctionIdAndStatus(anyLong(), anyLong(), eq(BidStatus.SUCCESS))).thenReturn(true);

		// when & then
		assertThatThrownBy(() -> bidService.attemptBid(userId, request))
			.isInstanceOf(CustomException.class)
			.hasMessageContaining(ErrorCode.AUCTION_ALREADY_PARTICIPATED.getMessage());
	}

	@Test
	@DisplayName("낙찰 시도 실패 - 락 획득 실패")
	void attemptBid_Fail_LockNotAcquired() throws InterruptedException {
		// given
		BidRequest request = new BidRequest(optionId, bidPrice);
		when(auctionOptionRepository.findByIdWithAuction(optionId)).thenReturn(Optional.of(option));
		when(redissonClient.getLock(anyString())).thenReturn(lock);
		// 락 획득 실패 모킹 (기존 lenient 설정을 덮어씀)
		when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(false);

		// when & then
		assertThatThrownBy(() -> bidService.attemptBid(userId, request))
			.isInstanceOf(CustomException.class)
			.hasMessageContaining("요청이 너무 많습니다");
	}
}
