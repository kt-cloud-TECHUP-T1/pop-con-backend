package com.t1.popcon.popup.detail.entity;

import com.t1.popcon.common.entity.BaseSoftDeleteEntity;
import com.t1.popcon.popup.dto.card.PhaseType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "popup")
@SQLRestriction("deleted = false")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class Popup extends BaseSoftDeleteEntity {

    @Column(name = "auction_id", nullable = false)
    private Long auctionId;

    @Column(name = "draw_id", nullable = false)
    private Long drawId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "sub_text", length = 255)
    private String subText;

    @Column(name = "caption", length = 255)
    private String caption;

    @Column(name = "subtitle", length = 255)
    private String subtitle;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "location", nullable = false, length = 255)
    private String location;

    @Column(name = "thumbnail_url", nullable = false, length = 500)
    private String thumbnailUrl;

    @OneToMany(mappedBy = "popup")
    @jakarta.persistence.OrderBy("sortOrder ASC, id ASC")
    @Builder.Default
    private List<PopupImage> images = new ArrayList<>();

    @Column(name = "open_at", nullable = false)
    private LocalDate openAt;

    @Column(name = "close_at", nullable = false)
    private LocalDate closeAt;

    @Column(name = "weekday_open_time", nullable = false)
    private LocalTime weekdayOpen;

    @Column(name = "weekday_close_time", nullable = false)
    private LocalTime weekdayClose;

    @Column(name = "weekend_open_time", nullable = false)
    private LocalTime weekendOpen;

    @Column(name = "weekend_close_time", nullable = false)
    private LocalTime weekendClose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PhaseType phaseType;

    @Column(name = "like_count", nullable = false)
    private Long likeCount;

    @Column(name = "view_count", nullable = false)
    private Long viewCount;

    @Column(name = "review_count", nullable = false)
    private Long reviewCount;
}
