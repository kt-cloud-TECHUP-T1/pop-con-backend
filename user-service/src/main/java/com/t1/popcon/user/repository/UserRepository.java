package com.t1.popcon.user.repository;

import com.t1.popcon.user.domain.Role;
import com.t1.popcon.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findById(Long id);

    Optional<User> findByCiHash(String ciHash);

    Optional<User> findByKakaoUserId(String kakaoUserId);
    Optional<User> findByNaverUserId(String naverUserId);

    Optional<User> findByPhoneHash(String phoneHash);

    Optional<User> findFirstByNicknameStartingWithOrderByIdDesc(String prefix);

    boolean existsByNickname(String nickname);

    /** 닉네임 중복 확인 - 본인 제외 (프로필 수정 시 사용) */
    boolean existsByNicknameAndIdNot(String nickname, Long id);

    long countByRole(Role role);

    Optional<User> findByNickname(String nickname);

    Optional<User> findByNicknameAndRole(String nickname, Role role);
}