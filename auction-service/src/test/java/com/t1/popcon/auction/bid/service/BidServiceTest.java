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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
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

	@InjectMocks
	private BidService bidService;

	private final Long userId = 1L;
	private final Long optionId = 100L;
	private final Integer bidPrice = 50000;
	private Auction auction;
	private AuctionOption option;

	@BeforeEach
	void setUp() {
		auction = mock(Auction.class);
		option = mock(AuctionOption.class);
		lenient().when(option.getId()).thenReturn(optionId);
		lenient().when(option.getAuction()).thenReturn(auction);
		lenient().when(auction.getId()).thenReturn(1L);
	}

	@Test
	@DisplayName("낙찰 시도 성공 - 모든 과정 정상")
	void attemptBid_Success() {
		// given
		BidRequest request = new BidRequest(optionId, bidPrice);
		given(auctionOptionRepository.findByIdWithAuction(optionId)).willReturn(Optional.of(option));
		given(bidRepository.existsByUserIdAndAuctionIdAndStatus(anyLong(), anyLong(), any())).willReturn(false);
		given(auctionStockService.hasAvailableStock(anyLong())).willReturn(true);
		given(auctionStockService.getPriceAnchor(anyLong())).willReturn(new AuctionStockService.PriceAnchor(null, null));
		given(auctionPriceService.calculateStatus(any(), any(), anyBoolean())).willReturn(AuctionStatus.OPEN);
		given(auctionPriceService.calculateCurrentPrice(any(), any(), any(), any(), any())).willReturn(bidPrice);
		given(bidRedisRepository.decrementStock(optionId)).willReturn(5L);

		String merchantUid = "merchant_123";
		Bid bid = Bid.builder()
			.userId(userId)
			.auctionOption(option)
			.bidPrice(bidPrice)
			.merchantUid(merchantUid)
			.build();
		ReflectionTestUtils.setField(bid, "id", 1L);
		given(txManager.preparePendingBid(anyLong(), any(), anyInt(), anyString())).willReturn(bid);

		BillingKeyInternalResponse billingKey = new BillingKeyInternalResponse("billing_key_abc");
		given(userBillingClient.getDefaultBillingKey(userId)).willReturn(ApiResponse.ok(billingKey));

		PortOnePaymentResponse.Payment paymentDetail = new PortOnePaymentResponse.Payment("pg_tx_123", "2023-11-20T15:30:00Z");
		PortOnePaymentResponse paymentResponse = new PortOnePaymentResponse(paymentDetail);
		given(portOneClient.executePayment(anyString(), anyString(), anyInt(), anyString())).willReturn(paymentResponse);

		PopupInternalResponse popupInfo = new PopupInternalResponse(1L, "Pop-up Title", "Location", "h_thumbnail.url", "v_thumbnail.url");
		given(auction.getPopupId()).willReturn(1L);
		given(popupServiceClient.getPopupDetail(anyLong())).willReturn(ApiResponse.ok(popupInfo));
		given(option.getEntryDate()).willReturn(LocalDate.now());
		given(option.getEntryTime()).willReturn(LocalTime.now());
		given(auction.getStartPrice()).willReturn(10000);
		given(reservationNoGenerator.generate()).willReturn("TKT123456789012");
		given(txManager.completeBidSuccess(anyLong(), anyLong(), anyString(), any(), anyString(), anyString(), anyString(), anyString(), any(), any(), anyInt()))
			.willReturn("TKT123456789012");
		given(ticketServiceClient.issueTicket(any())).willReturn(
			ApiResponse.ok(new TicketIssueResponse(
				1L,
				"TKT00000001",
				"TKT123456789012",
				"ISSUED",
				"AUCTION",
				1L
			))
		);

		// when
		BidResponse response = bidService.attemptBid(userId, request);

		// then
		assertThat(response.status()).isEqualTo(BidStatus.SUCCESS);
		verify(txManager).completeBidSuccess(
			eq(bid.getId()),
			eq(optionId),
			eq("pg_tx_123"),
			any(LocalDateTime.class),
			eq("TKT123456789012"),
			eq("Pop-up Title"),
			eq("Location"),
			eq("v_thumbnail.url"),
			any(LocalDate.class),
			any(LocalTime.class),
			eq(10000)
		);
		verify(ticketServiceClient).issueTicket(any());
	}

	@Test
	@DisplayName("낙찰 시도 실패 - 재고 없음")
	void attemptBid_Fail_SoldOut() {
		// given
		BidRequest request = new BidRequest(optionId, bidPrice);
		given(auctionOptionRepository.findByIdWithAuction(optionId)).willReturn(Optional.of(option));
		given(bidRepository.existsByUserIdAndAuctionIdAndStatus(anyLong(), anyLong(), any())).willReturn(false);
		given(auctionStockService.hasAvailableStock(anyLong())).willReturn(true);
		given(auctionStockService.getPriceAnchor(anyLong())).willReturn(new AuctionStockService.PriceAnchor(null, null));
		given(auctionPriceService.calculateStatus(any(), any(), anyBoolean())).willReturn(AuctionStatus.OPEN);
		given(auctionPriceService.calculateCurrentPrice(any(), any(), any(), any(), any())).willReturn(bidPrice);
		given(bidRedisRepository.decrementStock(optionId)).willReturn(-1L);

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
		given(auctionOptionRepository.findByIdWithAuction(optionId)).willReturn(Optional.of(option));
		given(bidRepository.existsByUserIdAndAuctionIdAndStatus(anyLong(), anyLong(), any())).willReturn(false);
		given(auctionStockService.hasAvailableStock(anyLong())).willReturn(true);
		given(auctionStockService.getPriceAnchor(anyLong())).willReturn(new AuctionStockService.PriceAnchor(null, null));
		given(auctionPriceService.calculateStatus(any(), any(), anyBoolean())).willReturn(AuctionStatus.OPEN);
		given(auctionPriceService.calculateCurrentPrice(any(), any(), any(), any(), any())).willReturn(bidPrice);
		given(bidRedisRepository.decrementStock(optionId)).willReturn(5L);

		Bid bid = Bid.builder()
			.userId(userId)
			.auctionOption(option)
			.bidPrice(bidPrice)
			.merchantUid("merchant_123")
			.build();
		ReflectionTestUtils.setField(bid, "id", 1L);
		given(txManager.preparePendingBid(anyLong(), any(), anyInt(), anyString())).willReturn(bid);

		BillingKeyInternalResponse billingKey = new BillingKeyInternalResponse("billing_key_abc");
		given(userBillingClient.getDefaultBillingKey(userId)).willReturn(ApiResponse.ok(billingKey));

		given(portOneClient.executePayment(anyString(), anyString(), anyInt(), anyString()))
			.willThrow(new RuntimeException("Payment API Error"));
		given(portOneClient.cancelPayment(anyString(), anyInt()))
			.willReturn(new PortOneCancelResponse(
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
		given(auctionOptionRepository.findByIdWithAuction(optionId)).willReturn(Optional.of(option));
		// 이미 낙찰 성공 내역이 있다고 가정 (true)
		given(bidRepository.existsByUserIdAndAuctionIdAndStatus(anyLong(), anyLong(), eq(BidStatus.SUCCESS))).willReturn(true);

		// when & then
		assertThatThrownBy(() -> bidService.attemptBid(userId, request))
			.isInstanceOf(CustomException.class)
			.hasMessageContaining(ErrorCode.AUCTION_ALREADY_PARTICIPATED.getMessage());
	}
}
