package com.t1.popcon.popup.banners.entity;

import com.t1.popcon.common.entity.BaseAuditEntity;
import com.t1.popcon.popup.detail.entity.Popup;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "banners")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Banner extends BaseAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "popup_id", nullable = false)
    private Popup popup;

    @Column(nullable = false, length = 255)
    private String supportingText;

    @Column(nullable = false)
    private int priority;

    @Column(nullable = false)
    private boolean isActive = true;
}
