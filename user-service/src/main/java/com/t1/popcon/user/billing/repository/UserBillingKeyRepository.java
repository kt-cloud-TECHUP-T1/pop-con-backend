package com.t1.popcon.user.billing.repository;

import com.t1.popcon.user.billing.entity.UserBillingKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserBillingKeyRepository extends JpaRepository<UserBillingKey, Long> {

	Optional<UserBillingKey> findByUserIdAndIsActiveTrue(Long userId);

	boolean existsByUserIdAndIsActiveTrue(Long userId);
}