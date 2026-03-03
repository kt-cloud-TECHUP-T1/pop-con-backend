package com.t1.popcon.common.entity;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;

@Getter
@MappedSuperclass
public abstract class BaseAuditEntity extends BaseTimeEntity {
	@CreatedBy
	protected Long createdBy;

	@LastModifiedBy
	protected Long updatedBy;
}