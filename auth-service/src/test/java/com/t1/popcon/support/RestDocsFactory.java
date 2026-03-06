package com.t1.popcon.support;

import static com.epages.restdocs.apispec.ResourceDocumentation.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.payload.PayloadDocumentation;
import org.springframework.restdocs.request.ParameterDescriptor;
import org.springframework.restdocs.request.RequestDocumentation;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper;
import com.epages.restdocs.apispec.ResourceSnippetParametersBuilder;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.epages.restdocs.apispec.Schema;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * RestDocs + restdocs-api-spec 연동을 위한 공통 팩토리 클래스
 * - MockMvc 요청 빌더
 * - 요청/응답 스키마 + 필드 자동 생성
 */
@Component
public class RestDocsFactory {

	/**
	 * HTTP 메서드, URL, 바디에 맞는 MockMvc 요청 생성
	 */
	public MockHttpServletRequestBuilder createRequest(
		String url,
		Object requestDto,
		HttpMethod method,
		ObjectMapper objectMapper,
		Object... pathParams
	) throws Exception {

		String content = requestDto != null ? objectMapper.writeValueAsString(requestDto) : "";
		return buildRequest(url, content, method, pathParams);
	}

	/**
	 * GET + QueryString 전용 요청 빌더
	 * - dto 를 쿼리 파라미터로 변환해서 붙여줌
	 * - Pageable 도 같이 받으면 page/size/sort 도 자동 추가
	 */
	public MockHttpServletRequestBuilder createParamRequest(
		String url,
		Object queryDto,
		Pageable pageable,
		ObjectMapper objectMapper,
		Object... pathParams
	) {
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();

		if (queryDto != null) {
			params.addAll(MultiValueMapConverter.convert(objectMapper, queryDto));
		}

		if (pageable != null) {
			params.add("page", String.valueOf(pageable.getPageNumber()));
			params.add("size", String.valueOf(pageable.getPageSize()));
			pageable.getSort().forEach(order -> params.add("sort", order.getProperty() + "," + order.getDirection().name()));
		}

		return RestDocumentationRequestBuilders.get(url, pathParams)
			.params(params)
			.accept(MediaType.APPLICATION_JSON);
	}

	/**
	 * 성공 케이스용 공통 문서 생성 (쿼리 파라미터 포함)
	 */
	public <Q, R> RestDocumentationResultHandler successWithRequestParameters(
		String identifier,
		String summary,
		String description,
		String tag,
		Q queryDto,
		Pageable pageable,
		ObjectMapper objectMapper,
		R responseDto
	) {
		String responseSchemaName = responseDto != null ? responseDto.getClass().getSimpleName() : null;
		List<ParameterDescriptor> queryParameters = buildParameterDescriptors(queryDto, pageable, objectMapper);

		ResourceSnippetParametersBuilder builder = ResourceSnippetParameters.builder()
			.tag(tag)
			.summary(summary)
			.description(description)
			.queryParameters(queryParameters.toArray(new ParameterDescriptor[0]));
		if (responseSchemaName != null) {
			builder.responseSchema(Schema.schema(responseSchemaName));
		}
		if (responseDto != null) {
			builder.responseFields(getFields(responseDto));
		}
		ResourceSnippetParameters resource = builder.build();

		return MockMvcRestDocumentationWrapper.document(
			identifier,
			preprocessResponse(prettyPrint()),
			resource(resource)
		);
	}

	// ========================================================================
	// Success Resource (단건)
	// ========================================================================

	/**
	 * 성공 케이스용 공통 문서 생성
	 */
	public <T, R> RestDocumentationResultHandler success(
		String identifier,
		String summary,
		String description,
		String tag,
		T requestDto,
		R responseDto
	) {
		String requestSchemaName = requestDto != null ? requestDto.getClass().getSimpleName() : null;
		String responseSchemaName = responseDto != null ? responseDto.getClass().getSimpleName() : null;

		if (requestDto == null) {
			return MockMvcRestDocumentationWrapper.document(
				identifier,
				preprocessResponse(prettyPrint()),
				resource(
					ResourceSnippetParameters.builder()
						.tag(tag)
						.summary(summary)
						.description(description)
						.responseSchema(responseSchemaName != null ? Schema.schema(responseSchemaName) : null)
						.responseFields(responseDto != null ? getFields(responseDto) : new FieldDescriptor[] {})
						.build()
				)
			);
		}

		return MockMvcRestDocumentationWrapper.document(
			identifier,
			preprocessRequest(prettyPrint()),
			preprocessResponse(prettyPrint()),
			resource(
				ResourceSnippetParameters.builder()
					.tag(tag)
					.summary(summary)
					.description(description)
					.requestSchema(Schema.schema(requestSchemaName))
					.responseSchema(responseSchemaName != null ? Schema.schema(responseSchemaName) : null)
					.requestFields(getFields(requestDto))
					.responseFields(responseDto != null ? getFields(responseDto) : new FieldDescriptor[] {})
					.build()
			)
		);
	}

	// ========================================================================
	// Failure Resource (실패 케이스)
	// ========================================================================

	/**
	 * 실패 케이스용 공통 문서 생성
	 */
	public <T, R> RestDocumentationResultHandler failure(
		String identifier,
		String summary,
		String description,
		String tag,
		T requestDto,
		R errorResponseDto
	) {
		String requestSchemaName = requestDto != null ? requestDto.getClass().getSimpleName() : null;
		String responseSchemaName = errorResponseDto != null ? errorResponseDto.getClass().getSimpleName() : "ErrorResponse";

		ResourceSnippetParametersBuilder builder = ResourceSnippetParameters.builder()
			.tag(tag)
			.summary(summary)
			.description(description);

		if (requestDto != null) {
			builder.requestSchema(Schema.schema(requestSchemaName))
				.requestFields(getFields(requestDto));
		}

		if (errorResponseDto != null) {
			builder.responseSchema(Schema.schema(responseSchemaName))
				.responseFields(getFields(errorResponseDto));
		}

		return MockMvcRestDocumentationWrapper.document(
			identifier,
			preprocessRequest(prettyPrint()),
			preprocessResponse(prettyPrint()),
			resource(builder.build())
		);
	}

	private MockHttpServletRequestBuilder buildRequest(
		String url,
		String content,
		HttpMethod method,
		Object... pathParams
	) {
		return switch (method.name()) {
			case "POST" -> RestDocumentationRequestBuilders.post(url, pathParams)
				.contentType(MediaType.APPLICATION_JSON)
				.content(content)
				.accept(MediaType.APPLICATION_JSON);
			case "PUT" -> RestDocumentationRequestBuilders.put(url, pathParams)
				.contentType(MediaType.APPLICATION_JSON)
				.content(content)
				.accept(MediaType.APPLICATION_JSON);
			case "PATCH" -> RestDocumentationRequestBuilders.patch(url, pathParams)
				.contentType(MediaType.APPLICATION_JSON)
				.content(content)
				.accept(MediaType.APPLICATION_JSON);
			case "GET" -> RestDocumentationRequestBuilders.get(url, pathParams)
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON);
			case "DELETE" -> RestDocumentationRequestBuilders.delete(url, pathParams)
				.contentType(MediaType.APPLICATION_JSON)
				.content(content)
				.accept(MediaType.APPLICATION_JSON);
			default -> throw new IllegalArgumentException("Invalid HTTP method: " + method);
		};
	}

	private List<ParameterDescriptor> buildParameterDescriptors(Object queryDto, Pageable pageable, ObjectMapper objectMapper) {
		List<ParameterDescriptor> parameters = new ArrayList<>();

		if (queryDto != null) {
			MultiValueMap<String, String> params = MultiValueMapConverter.convert(objectMapper, queryDto);
			params.forEach((key, value) -> parameters.add(
				RequestDocumentation.parameterWithName(key).description(key).optional()
			));
		}

		if (pageable != null) {
			parameters.add(RequestDocumentation.parameterWithName("page").description("page").optional());
			parameters.add(RequestDocumentation.parameterWithName("size").description("size").optional());
			parameters.add(RequestDocumentation.parameterWithName("sort").description("sort").optional());
		}

		return parameters;
	}

	// ========================================================================
	// Field 자동 생성
	// ========================================================================

	/**
	 * DTO 구조를 기반으로 JsonFieldDescriptor 배열을 생성
	 */
	public <T> FieldDescriptor[] getFields(T dto) {
		List<FieldDescriptor> fields = new ArrayList<>();
		Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
		if (dto instanceof Collection<?> collection) {
			fields.add(PayloadDocumentation.fieldWithPath("[]")
				.type(JsonFieldType.ARRAY)
				.description("root array")
				.optional());
			if (!collection.isEmpty()) {
				Object first = collection.iterator().next();
				generateFieldDescriptors(first, "[].", fields, visited);
			}
			return fields.toArray(new FieldDescriptor[0]);
		}
		if (dto != null && dto.getClass().isArray()) {
			fields.add(PayloadDocumentation.fieldWithPath("[]")
				.type(JsonFieldType.ARRAY)
				.description("root array")
				.optional());
			if (Array.getLength(dto) > 0) {
				generateFieldDescriptors(Array.get(dto, 0), "[].", fields, visited);
			}
			return fields.toArray(new FieldDescriptor[0]);
		}
		generateFieldDescriptors(dto, "", fields, visited);
		return fields.toArray(new FieldDescriptor[0]);
	}

	/**
	 * 리플렉션으로 필드를 순회하며 필드/서브필드를 자동 문서화
	 * - Map 타입(nodesById 등)은 subsection으로만 문서화하고 내부 키는 스키마에서 제외
	 * - page, slice 같은 필드는 optional 로 선언하여 JSON에 없어도 에러가 나지 않도록 처리
	 */
	private <T> void generateFieldDescriptors(T dto, String pathPrefix, List<FieldDescriptor> fields, Set<Object> visited) {
		if (dto == null || isSimpleType(dto) || visited.contains(dto)) {
			return;
		}

		Field[] declaredFields = dto.getClass().getDeclaredFields();
		for (Field field : declaredFields) {
			field.setAccessible(true);
			String fieldPath = pathPrefix + field.getName();
			Object fieldValue = getFieldValue(dto, field);
			Class<?> fieldTypeClass = field.getType();

			if (fieldValue instanceof Map<?, ?> || Map.class.isAssignableFrom(fieldTypeClass)) {
				FieldDescriptor descriptor = PayloadDocumentation
					.subsectionWithPath(fieldPath)
					.type(JsonFieldType.OBJECT)
					.description(field.getName())
					.optional();
				fields.add(descriptor);
				continue;
			}

			JsonFieldType fieldType = determineFieldType(fieldTypeClass, fieldValue);
			FieldDescriptor descriptor = PayloadDocumentation.fieldWithPath(fieldPath)
				.type(fieldType)
				.description(field.getName())
				.optional();
			fields.add(descriptor);

			if (fieldType == JsonFieldType.ARRAY) {
				Object firstElement = null;
				if (fieldValue instanceof List<?> list && !list.isEmpty()) {
					firstElement = list.get(0);
				} else if (fieldValue instanceof Collection<?> collection && !collection.isEmpty()) {
					firstElement = collection.iterator().next();
				} else if (fieldValue != null && fieldValue.getClass().isArray() && Array.getLength(fieldValue) > 0) {
					firstElement = Array.get(fieldValue, 0);
				}
				if (firstElement != null) {
					generateFieldDescriptors(firstElement, fieldPath + "[].", fields, visited);
				}
			}

			if (fieldType == JsonFieldType.OBJECT && fieldValue != null && !isSimpleType(fieldValue)) {
				generateFieldDescriptors(fieldValue, fieldPath + ".", fields, visited);
			}
		}
	}

	private <T> Object getFieldValue(T dto, Field field) {
		try {
			return field.get(dto);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Failed to access field: " + field.getName(), e);
		}
	}

	/**
	 * 더 이상 필드를 파고들지 않을 단순 타입 여부 판단
	 */
	private boolean isSimpleType(Object dto) {
		if (dto == null) {
			return true;
		}

		Class<?> type = dto.getClass();
		if (type.isPrimitive() || type.isEnum()) {
			return true;
		}

		Package pkg = type.getPackage();
		if (pkg != null && pkg.getName().startsWith("java.")) {
			return true;
		}

		return dto instanceof String || dto instanceof Number || dto instanceof Boolean;
	}

	private JsonFieldType determineFieldType(Class<?> fieldType, Object fieldValue) {
		if (fieldValue instanceof Collection<?> || (fieldValue != null && fieldValue.getClass().isArray())) {
			return JsonFieldType.ARRAY;
		}
		if (Map.class.isAssignableFrom(fieldType)) {
			return JsonFieldType.OBJECT;
		}
		if (fieldType == String.class || fieldType.isEnum()) {
			return JsonFieldType.STRING;
		}
		if (Boolean.class.isAssignableFrom(fieldType) || fieldType == boolean.class) {
			return JsonFieldType.BOOLEAN;
		}
		if (Number.class.isAssignableFrom(fieldType) || fieldType.isPrimitive()) {
			return JsonFieldType.NUMBER;
		}
		if (Collection.class.isAssignableFrom(fieldType) || fieldType.isArray()) {
			return JsonFieldType.ARRAY;
		}
		if (fieldType == LocalDate.class || fieldType == LocalDateTime.class || fieldType == LocalTime.class) {
			return JsonFieldType.STRING;
		}
		return JsonFieldType.OBJECT;
	}
}