package com.t1.popcon.user.repository;

import com.t1.popcon.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByCiHashAndDeletedFalse(String ciHash);

    Optional<User> findByKakaoUserIdAndDeletedFalse(String kakaoUserId);
    Optional<User> findByNaverUserIdAndDeletedFalse(String naverUserId);
}