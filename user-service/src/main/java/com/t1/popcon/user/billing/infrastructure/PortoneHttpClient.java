package com.t1.popcon.user.billing.infrastructure;

import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import com.t1.popcon.user.billing.dto.PortoneBillingKeyResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
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
				log.info(">>>> [Portone V2 Request] {} {}", request.getMethod(), request.getURI());
				return execution.execute(request, body);
			})
			.build();
	}

	@Override
	public PortoneBillingKeyResponse fetchBillingKeyInfo(String billingKey) {
		return restClient.get()
			.uri("/billing-keys/{billingKey}", billingKey)
			.header("Authorization", "Portone " + apiSecret.trim())
			.retrieve()
			.onStatus(HttpStatusCode::isError, (req, res) -> {
				log.error("Portone API Error: {}", res.getStatusCode());
				throw new CustomException(ErrorCode.PAYMENT_FETCH_FAILED);
			})
			.body(PortoneBillingKeyResponse.class);
	}
}