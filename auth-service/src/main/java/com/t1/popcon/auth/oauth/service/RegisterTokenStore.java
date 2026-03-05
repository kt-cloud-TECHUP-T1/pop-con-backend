package com.t1.popcon.auth.oauth.service;

import com.t1.popcon.auth.oauth.dto.RegisterPayload;
import java.util.Optional;

public interface RegisterTokenStore {

    void save(String registerToken, RegisterPayload payload, long ttlSeconds);

    Optional<RegisterPayload> find(String registerToken);

    /**
     * 본인인증 완료 시 CI Hash를 registerToken payload에 반영(업데이트)
     */
    void mergeCiHash(String registerToken, String ciHash, long ttlSecondsToExtend);

    /**
     * 가입 완료 시 registerToken 제거(재사용 방지)
     */
    void delete(String registerToken);
}