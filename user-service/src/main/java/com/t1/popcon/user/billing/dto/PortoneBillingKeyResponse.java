package com.t1.popcon.user.billing.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PortoneBillingKeyResponse(
	@JsonProperty("billingKey") String billingKey, // V2 필드명 매핑
	String status,
	List<Method> methods,   // 배열 형태 대응
	List<Channel> channels  // 배열 형태 대응
) {
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Method(
		String type,
		Card card
	) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Card(
		String name,
		String number
	) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Channel(
		String pgProvider
	) {}

	// 엔티티 저장을 위한 헬퍼 메서드
	public String getPgProvider() {
		return (channels != null && !channels.isEmpty()) ? channels.get(0).pgProvider() : "UNKNOWN";
	}

	public String getCardName() {
		if (methods != null && !methods.isEmpty() && methods.get(0).card() != null) {
			return methods.get(0).card().name();
		}
		return "알 수 없는 카드";
	}

	public String getCardNumber() {
		if (methods != null && !methods.isEmpty() && methods.get(0).card() != null) {
			return methods.get(0).card().number();
		}
		return "****-****-****-****";
	}
}