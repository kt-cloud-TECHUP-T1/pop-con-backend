package com.t1.popcon.popup.detail.entity;

import com.t1.popcon.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
        name = "popup_likes",
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_popup_like_user", columnNames = {"popup_id", "user_id"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PopupLike extends BaseTimeEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "popup_id", nullable = false)
    private Popup popup;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    private PopupLike(Popup popup, Long userId) {
        this.popup = popup;
        this.userId = userId;
    }

    public static PopupLike create(Popup popup, Long userId) {
        return new PopupLike(popup, userId);
    }
}