package com.t1.popcon.magazine.entity;

import com.t1.popcon.common.entity.BaseSoftDeleteEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "magazine")
@SQLRestriction("deleted = false")
public class Magazine extends BaseSoftDeleteEntity {

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "supporting_text", nullable = false, length = 255)
    private String supportingText;

    @Column(name = "thumbnail_url", nullable = false, length = 500)
    private String thumbnailUrl;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    @Builder
    public Magazine(String title, String supportingText, String thumbnailUrl, LocalDateTime publishedAt) {
        this.title = title;
        this.supportingText = supportingText;
        this.thumbnailUrl = thumbnailUrl;
        this.publishedAt = publishedAt;
    }
}
