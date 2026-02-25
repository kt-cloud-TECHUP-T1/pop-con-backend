package com.t1.popcon.support;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MultiValueMapConverter {

	/**
	 * DTO → MultiValueMap<String, String> 변환
	 *
	 * - @JsonProperty, @JsonFormat 등 Jackson 설정은 그대로 사용
	 * - null 값은 제외
	 * - Collection/배열은 같은 key로 여러 번 add 허용 (a=1&a=2&a=3)
	 */
	public static MultiValueMap<String, String> convert(ObjectMapper objectMapper, Object dto) {
		try {
			// 1단계: DTO → Map<String, Object>
			Map<String, Object> raw = objectMapper.convertValue(
				dto,
				new TypeReference<Map<String, Object>>() {}
			);

			MultiValueMap<String, String> params = new LinkedMultiValueMap<>();

			raw.forEach((key, value) -> {
				if (value == null) {
					return; // null은 쿼리스트링에 넣지 않음
				}

				if (value instanceof Collection<?> collection) {
					// List / Set 등
					collection.forEach(element -> addValue(params, key, element));
				} else if (value.getClass().isArray()) {
					// 배열
					int length = Array.getLength(value);
					for (int i = 0; i < length; i++) {
						Object element = Array.get(value, i);
						addValue(params, key, element);
					}
				} else {
					// 단일 값
					addValue(params, key, value);
				}
			});

			return params;
		} catch (Exception e) {
			log.error("Url Parameter 변환중 오류가 발생했습니다. requestDto={}", dto, e);
			throw new IllegalStateException("Url Parameter 변환중 오류가 발생했습니다.", e);
		}
	}

	private static void addValue(MultiValueMap<String, String> params, String key, Object value) {
		if (value == null) {
			return;
		}
		// Jackson이 이미 String으로 바꿔준 경우가 많지만, 아닐 수도 있으니 toString 사용
		params.add(key, value.toString());
	}
}