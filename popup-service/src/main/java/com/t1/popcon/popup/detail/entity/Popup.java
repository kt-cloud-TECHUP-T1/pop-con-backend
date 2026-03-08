package com.t1.popcon.popup.detail.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

import com.t1.popcon.common.entity.BaseSoftDeleteEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class Popup extends BaseSoftDeleteEntity {

	private Boolean liked;	// 좋아요 여부

	@Column(nullable = false, length = 255)
	private String location; // 주소 정보

	@Column(nullable = false, columnDefinition = "TEXT")
	private String description; // 스토어 상세정보

	@Column(nullable = false)
	private String title; // 스토어 이름

	private String subtitle; // 스토어 부제목

	private String supportingText; // 배너용

	private String caption; // 배너/카드용

	private String subText; // 카드용

	@Column(nullable = false)
	private String thumbnailUrl; // 썸네일 URL

	@ElementCollection
	@CollectionTable(name = "popup_images", joinColumns = @JoinColumn(name = "popup_id"))
	@Column(name = "image_url")
	private List<String> images; // 상세페이지 이미지 링크 모음

	@Column(nullable = false)
	private LocalDateTime openAt; // 팝업 오픈일

	@Column(nullable = false)
	private LocalDateTime closeAt; // 팝업 종료일

	@Column(nullable = false)
	private LocalDateTime weekdayOpen;	// 평일 오픈 시간

	@Column(nullable = false)
	private LocalDateTime weekdayClose;	// 평일 종료 시간

	@Column(nullable = false)
	private LocalDateTime weekendOpen;	// 주말 오픈 시간

	@Column(nullable = false)
	private LocalDateTime weekendClose;	// 주말 종료 시간

	@Column(nullable = false)
	private LocalDateTime auctionOpenAt; // 경매 오픈일

	@Column(nullable = false)
	private LocalDateTime auctionCloseAt; // 경매 종료일

	@Column(nullable = false)
	private LocalDateTime drawOpenAt; // 드로우 오픈일

	@Column(nullable = false)
	private LocalDateTime drawCloseAt; // 드로우 종료일

	@Column(nullable = false)
	private Long startPrice; // 경매 시작가

	@Column(nullable = false)
	private Long extraTicket; // 회차당 티켓 수량

}