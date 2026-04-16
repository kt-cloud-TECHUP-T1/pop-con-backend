package com.t1.popcon.user.billing.repository;

import com.t1.popcon.user.billing.entity.UserBillingKey;
import com.t1.popcon.user.domain.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserBillingKeyRepository extends JpaRepository<UserBillingKey, Long> {

	boolean existsByUserAndIsActiveTrue(User user);

	List<UserBillingKey> findAllByUserAndIsActiveTrue(User user);

	Optional<UserBillingKey> findByUserAndIsDefaultTrueAndIsActiveTrue(User user);

	boolean existsByUserAndCardNumberAndCardNameAndPgProviderAndIsActiveTrue(
		User user, String cardNumber, String cardName, String pgProvider);
}