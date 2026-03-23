package com.t1.popcon.draw.domain;

import com.t1.popcon.common.entity.BaseSoftDeleteEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@Table(
	name = "draw_entries",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_user_draw_option",
			columnNames = {"user_id", "draw_option_id", "deleted"}
		)
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted = false")
public class DrawEntry extends BaseSoftDeleteEntity {

	@Column(nullable = false)
	private Long userId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "draw_option_id", nullable = false)
	private DrawOption drawOption;

	@Builder
	public DrawEntry(Long userId, DrawOption drawOption) {
		this.userId = userId;
		this.drawOption = drawOption;
	}
}