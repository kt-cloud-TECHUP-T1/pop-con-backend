package com.t1.popcon.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
public abstract class BaseSoftDeleteEntity extends BaseAuditEntity {

	@Column(nullable = false)
	protected boolean deleted = false;

	protected LocalDateTime deletedAt;

	protected Long deletedBy;

	public void markDeleted(Long deleterId) {
		this.deleted = true;
		this.deletedAt = LocalDateTime.now();
		this.deletedBy = deleterId;
	}
}