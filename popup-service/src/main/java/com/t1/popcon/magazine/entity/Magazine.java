package com.t1.popcon.magazine.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "magazine")
public class Magazine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "magazine_id")
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "supporting_text", nullable = false, length = 255)
    private String supportingText;

    @Column(name = "thumbnail_url", nullable = false, length = 500)
    private String thumbnailUrl;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    @Column(nullable = false)
    private boolean deleted;

    @Builder
    public Magazine(String title, String supportingText, String thumbnailUrl, LocalDateTime publishedAt, boolean deleted) {
        this.title = title;
        this.supportingText = supportingText;
        this.thumbnailUrl = thumbnailUrl;
        this.publishedAt = publishedAt;
        this.deleted = deleted;
    }
}
