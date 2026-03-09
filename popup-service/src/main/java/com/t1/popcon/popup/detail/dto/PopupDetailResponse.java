package com.t1.popcon.popup.detail.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import com.t1.popcon.popup.dto.card.PhaseStatus;
import com.t1.popcon.popup.dto.card.PhaseType;

import lombok.Builder;

@Builder
public record PopupDetailResponse(
	PhaseType phaseType,       // AUCTION, DRAW 등 [cite: 10]
	PhaseStatus phaseStatus,   // UPCOMING, OPEN, CLOSED 등 [cite: 10]
	Long popupId,              // 팝업스토어 고유 ID [cite: 2]
	Boolean liked,             // 현재 사용자의 좋아요 여부 [cite: 10]
	String thumbnailUrl,       // 대표 썸네일 이미지 링크 [cite: 2]
	List<String> images,       // 상세 이미지 리스트 (배열)
	String subtitle,           // 서브 타이틀
	String title,              // 팝업 스토어 이름
	Long viewCount,            // 조회수
	Long likeCount,            // 좋아요 수
	String description,        // 상세 설명 (TEXT)
	String location,           // 위치 정보 (주소)
	Long reviewCount,          // 리뷰 개수
	LocalDate openAt,          // 팝업 시작일
	LocalDate closeAt,         // 팝업 종료일
	LocalTime weekdayOpen,     // 평일 오픈 시간
	LocalTime weekdayClose,    // 평일 마감 시간
	LocalTime weekendOpen,     // 주말 오픈 시간
	LocalTime weekendClose,     // 주말 마감 시간

	// 중첩 DTO
	AuctionUpcoming auctionUpcoming,
	AuctionOpen auctionOpen,
	DrawUpcoming drawUpcoming,
	DrawOpen drawOpen
) {
	public PopupDetailResponse {
		if (images == null) {
			images = List.of();
		}
		if (liked == null) {
			liked = false;
		}
	}
	public static PopupDetailResponse ofMock(Long id) {
		return PopupDetailResponse.builder()
			.popupId(id) // 파라미터로 받은 ID 적용
			.phaseType(PhaseType.AUCTION)
			.phaseStatus(PhaseStatus.UPCOMING)
			.liked(true)
			.thumbnailUrl("https://imagelink.com/thumbnail.jpg")
			.images(List.of(
				"https://imagelink.com/image1.jpg",
				"https://imagelink.com/image2.jpg",
				"https://imagelink.com/image3.jpg"
			))
			.subtitle("Oneira X POPUP SEOUL")
			.title("오네이라 팝업 스토어")
			.viewCount(200L)
			.likeCount(200L)
			.description("""
												Lee가 101 라인 101주년을 기념해 더현대 서울에서 팝업을 진행합니다.👖
												오랜 시간 쌓아온 데님 헤리티지와 101 라인의 상징성을 공간 안에 풀어냈습니다.
												다양한 리워드와 팝업 기간동안의 특별한 혜택을 받아보세요!🩵
												
												[EVENT]
												① 02.18 PM 1:01 선착순 이벤트
												팝업 스토어 촬영 후 인스타그램 스토리 업로드 시, 선착순 101명 볼캡 증정
												② 구매 금액대별 혜택
												40만원 이상 → 반다나 반팔 티셔츠(20명)
												20만원 이상 → 빅 트위치 로고 투웨이 에코백(30명)
												10만원 이상 → 오버롤 미니 파우치(30명)
												제품 구매 고객 → 금속 뱃지 세트(200명) *ACC 제외
												③ 럭키드로우 이벤트
												Lee 공식 인스타그램 팔로우하고 스토리 업로드 시 : 럭키드로우 참여 기회 제공
												1등 셀비지 셋업(3명)
												2등 Lee 빈티지 라벨 티셔츠(5명)
												3등 스몰 트위치 로고 데님 볼캡(10명)
												4등 오버롤 키링(20명)
												5등 101 원형 뱃지 *컬러 랜덤(1000명)
												
												[INFO]
												📍장소 : 서울 영등포구 여의대로 108, 더현대 서울
												📅일정 : 2/18(수) ~ 2/25(수)
												⏰시간 : 월-목 10:30 ~ 20:00 / 금-일 10:30 ~ 20:30
                """)
			.location("서울 영등포구 여의대로 108, 더현대 서울")
			.reviewCount(0L)
			.openAt(LocalDate.of(2026, 2, 15))
			.closeAt(LocalDate.of(2026, 3, 15))
			.weekdayOpen(LocalTime.of(11, 0))
			.weekdayClose(LocalTime.of(21, 0))
			.weekendOpen(LocalTime.of(10, 0))
			.weekendClose(LocalTime.of(22, 0))
			.auctionUpcoming(new AuctionUpcoming(
				LocalDateTime.of(2026, 2, 9, 10, 0),
				132000L
			))
			.build();
	}

	public record AuctionUpcoming(
		LocalDateTime auctionOpenAt,
		Long startPrice
	){}
	public record AuctionOpen(
		LocalDateTime auctionCloseAt,
		Long currentPrice,
		Long extraTicket
	){}
	public record DrawUpcoming(
		LocalDateTime drawOpenAt
	){}
	public record DrawOpen(
		LocalDateTime drawCloseAt
	){}
}
