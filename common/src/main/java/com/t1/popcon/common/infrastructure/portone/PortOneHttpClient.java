package com.t1.popcon.common.infrastructure.portone;

import java.util.Map;

import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.common.infrastructure.dto.PortOneBillingKeyResponse;
import com.t1.popcon.common.infrastructure.dto.PortOneCancelResponse;
import com.t1.popcon.common.infrastructure.dto.PortOneIdentityVerificationResponse;
import com.t1.popcon.common.infrastructure.dto.PortOnePaymentResponse;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Profile("!local")
@Slf4j
public class PortOneHttpClient implements PortOneClient {

	private final RestClient restClient;

	@Value("${portone.api.secret}")
	private String apiSecret;

	@Value("${portone.api.store-id}")
	private String storeId;

	public PortOneHttpClient(@Value("${portone.url}") String baseUrl) {
		if (baseUrl == null || baseUrl.isBlank()) {
			throw new IllegalStateException("portone.url must be configured");
		}
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(3000);
		requestFactory.setReadTimeout(5000);
		this.restClient = RestClient.builder()
			.baseUrl(baseUrl)
			.requestFactory(requestFactory)
			.requestInterceptor((request, body, execution) -> {
				log.info(">>>> [PortOne V2 Request] Method: {}", request.getMethod());
				return execution.execute(request, body);
			})
			.build();
	}

	@PostConstruct
	void validateConfig() {
		if (apiSecret == null || apiSecret.isBlank()) {
			throw new IllegalStateException("portone.api.secret must be configured");
		}
		if (storeId == null || storeId.isBlank()) {
			throw new IllegalStateException("portone.api.store-id must be configured");
		}
	}

	@Override
	public PortOneBillingKeyResponse fetchBillingKeyInfo(String billingKey) {
		try {
			PortOneBillingKeyResponse response = restClient.get()
				.uri("/billing-keys/{billingKey}", billingKey)
				.header("Authorization", "PortOne " + apiSecret.trim())
				.retrieve()
				.onStatus(HttpStatusCode::isError, (req, res) -> {
					log.error(">>>> [PortOne API HTTP Error] Status: {}", res.getStatusCode());
					throw new CustomException(ErrorCode.PAYMENT_FETCH_FAILED);
				})
				.body(PortOneBillingKeyResponse.class);

			if (response == null) {
				log.error(">>>> [PortOne API Error] Response body is null");
				throw new CustomException(ErrorCode.PAYMENT_FETCH_FAILED);
			}
			return response;
		} catch (CustomException e) {
			throw e;
		} catch (Exception e) {
			log.error(">>>> [PortOne API Connection/Parsing Error] Message: {}", e.getMessage());
			throw new CustomException(ErrorCode.PAYMENT_FETCH_FAILED);
		}
	}

	@Override
	public PortOnePaymentResponse executePayment(String billingKey, String merchantUid, int amount, String orderName) {
		try {
			PortOnePaymentResponse response = restClient.post()
				.uri("/payments/{merchantUid}/billing-key", merchantUid)
				.header("Authorization", "PortOne " + apiSecret.trim())
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of(
					"billingKey", billingKey,
					"orderName", orderName,
					"amount", Map.of(
						"total", amount
					),
					"currency", "KRW",
					"storeId", storeId
				))
				.retrieve()
				.onStatus(HttpStatusCode::isError, (req, res) -> {
					log.error(">>>> [PortOne Payment Error] Status: {}, MerchantUid: {}", res.getStatusCode(), merchantUid);
					throw new CustomException(ErrorCode.PAYMENT_FETCH_FAILED);
				})
				.body(PortOnePaymentResponse.class);

			if (response == null || response.payment() == null) {
				log.error(">>>> [PortOne Payment Error] Response body is null or missing payment object, MerchantUid: {}", merchantUid);
				throw new CustomException(ErrorCode.PAYMENT_FETCH_FAILED);
			}

			log.info(">>>> [PortOne Payment API Call Success] MerchantUid: {}, paidAt: {}", merchantUid, response.payment().paidAt());
			return response;
		} catch (CustomException e) {
			throw e;
		} catch (Exception e) {
			log.error(">>>> [PortOne Payment Exception] MerchantUid: {}, Message: {}", merchantUid, e.getMessage());
			throw new CustomException(ErrorCode.PAYMENT_FETCH_FAILED);
		}
	}

	@Override
	public PortOneCancelResponse cancelPayment(String paymentId, int amount) {
		try {
			PortOneCancelResponse response = restClient.post()
				.uri("/payments/{paymentId}/cancel", paymentId)
				.header("Authorization", "PortOne " + apiSecret.trim())
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of(
					"reason", "티켓 예매 취소",
					"amount", amount
				))
				.retrieve()
				.onStatus(HttpStatusCode::isError, (req, res) -> {
					log.error(">>>> [PortOne Cancel Error] Status: {}, PaymentId: {}", res.getStatusCode(), paymentId);
					throw new CustomException(ErrorCode.PAYMENT_CANCEL_FAILED);
				})
				.body(PortOneCancelResponse.class);

			if (response == null || response.cancellation() == null) {
				log.error(">>>> [PortOne Cancel Error] Response body is null or invalid");
				throw new CustomException(ErrorCode.PAYMENT_CANCEL_FAILED);
			}

			log.info(">>>> [PortOne Cancel Success] PaymentId: {}, status: {}", paymentId, response.status());
			return response;
		} catch (CustomException e) {
			throw e;
		} catch (Exception e) {
			log.error(">>>> [PortOne Cancel Exception] PaymentId: {}, Message: {}", paymentId, e.getMessage());
			throw new CustomException(ErrorCode.PAYMENT_CANCEL_FAILED);
		}
	}

    /**
     * 포트원 본인인증 결과 조회 (S2S API)
     * @param identityVerificationId 본인인증 식별자
     * @return 본인인증 응답
     */
	@Override
	public PortOneIdentityVerificationResponse fetchIdentityVerification(String identityVerificationId) {
		try {
			PortOneIdentityVerificationResponse response = restClient.get()
				.uri("/identity-verifications/{identityVerificationId}", identityVerificationId)
				.header("Authorization", "PortOne " + apiSecret.trim())
				.retrieve()
				.onStatus(HttpStatusCode::isError, (req, res) -> {
					log.error(">>>> [PortOne Identity Verification API Error] Status: {}", res.getStatusCode());
					throw new CustomException(ErrorCode.IDENTITY_VERIFICATION_FETCH_FAILED);
				})
				.body(PortOneIdentityVerificationResponse.class);

			if (response == null) {
				log.error(">>>> [PortOne Identity Verification API Error] Response body is null");
				throw new CustomException(ErrorCode.IDENTITY_VERIFICATION_FETCH_FAILED);
			}

			log.info(">>>> [PortOne Identity Verification Response] status={}", response.status());

			return response;
		} catch (CustomException e) {
			throw e;
		} catch (Exception e) {
			log.error(">>>> [PortOne Identity Verification Error] Message: {}", e.getMessage());
			throw new CustomException(ErrorCode.IDENTITY_VERIFICATION_FETCH_FAILED);
		}
	}
}
