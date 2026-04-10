package com.t1.popcon.popup.categories.entity;

import com.t1.popcon.common.entity.BaseAuditEntity;
import com.t1.popcon.popup.detail.entity.Popup;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "categories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category extends BaseAuditEntity {

    // 아이콘 이미지 URL (없으면 null, 프론트에서 기본 이미지 대체)
    @Column(nullable = true, length = 512)
    private String iconUrl;

    // 아이콘 이름 (카테고리 라벨)
    @Column(nullable = false, length = 50)
    private String iconName;

    // 연결된 팝업
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "popup_id", nullable = false)
    private Popup popup;

    // 노출 순서
    @Column(nullable = false)
    private int priority;

    // 활성 여부
    @Column(nullable = false)
    private boolean isActive = true;
}
