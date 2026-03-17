package com.t1.popcon.user.billing.repository;

import com.t1.popcon.user.billing.entity.UserBillingKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;

@Repository
public interface UserBillingKeyRepository extends JpaRepository<UserBillingKey, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<UserBillingKey> findByUserIdAndIsActiveTrue(Long userId);

	boolean existsByUserIdAndIsActiveTrue(Long userId);
}