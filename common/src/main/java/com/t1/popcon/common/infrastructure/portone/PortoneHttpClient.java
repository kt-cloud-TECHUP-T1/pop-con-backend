package com.t1.popcon.common.infrastructure.portone;

import java.util.Map;

import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.common.infrastructure.dto.PortoneBillingKeyResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class PortoneHttpClient implements PortoneClient {

	private final RestClient restClient;

	@Value("${portone.api.secret}")
	private String apiSecret;

	public PortoneHttpClient(@Value("${portone.url}") String baseUrl) {
		this.restClient = RestClient.builder()
			.baseUrl(baseUrl)
			.requestInterceptor((request, body, execution) -> {
				log.info(">>>> [Portone V2 Request] {}", request.getMethod());
				return execution.execute(request, body);
			})
			.build();
	}

	@Override
	public PortoneBillingKeyResponse fetchBillingKeyInfo(String billingKey) {
		try {
			PortoneBillingKeyResponse response = restClient.get()
				.uri("/billing-keys/{billingKey}", billingKey)
				.header("Authorization", "Portone " + apiSecret.trim())
				.retrieve()
				.onStatus(HttpStatusCode::isError, (req, res) -> {
					log.error(">>>> [Portone API HTTP Error] Status: {}", res.getStatusCode());
					throw new CustomException(ErrorCode.PAYMENT_FETCH_FAILED);
				})
				.body(PortoneBillingKeyResponse.class);

			if (response == null) {
				log.error(">>>> [Portone API Error] Response body is null");
				throw new CustomException(ErrorCode.PAYMENT_FETCH_FAILED);
			}
			return response;
		} catch (CustomException e) {
			throw e;
		} catch (Exception e) {
			log.error(">>>> [Portone API Connection/Parsing Error] Message: {}", e.getMessage());
			throw new CustomException(ErrorCode.PAYMENT_FETCH_FAILED);
		}
	}

	@Override
	public void executePayment(String billingKey, String merchantUid, int amount, String orderName) {
		try {
			restClient.post()
				.uri("/payments/{merchantUid}/billing-key", merchantUid)
				.header("Authorization", "Portone " + apiSecret.trim())
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of(
					"billingKey", billingKey,
					"amount", Map.of(
						"total", amount
					),
					"orderName", orderName,
					"currency", "KRW"
				))
				.retrieve()
				.onStatus(HttpStatusCode::isError, (req, res) -> {
					log.error(">>>> [Portone Payment Error] Status: {}, MerchantUid: {}", res.getStatusCode(), merchantUid);
					throw new CustomException(ErrorCode.PAYMENT_FETCH_FAILED);
				})
				.toBodilessEntity();

			log.info(">>>> [Portone Payment Success] MerchantUid: {}", merchantUid);

		} catch (Exception e) {
			log.error(">>>> [Portone Payment Exception] MerchantUid: {}, Message: {}", merchantUid, e.getMessage());
			throw new CustomException(ErrorCode.PAYMENT_FETCH_FAILED);
		}
	}
}