package com.t1.popcon.user.repository;

import com.t1.popcon.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findById(Long id);

    Optional<User> findByCiHash(String ciHash);

    Optional<User> findByKakaoUserId(String kakaoUserId);
    Optional<User> findByNaverUserId(String naverUserId);

    Optional<User> findByPhoneHash(String phoneHash);

    boolean existsByNickname(String nickname);
    }