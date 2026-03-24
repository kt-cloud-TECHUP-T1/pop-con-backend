package com.t1.popcon.common.encryption;

import com.t1.popcon.common.exception.CustomException;
import com.t1.popcon.common.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 데이터 암호화/복호화 및 해시 처리 서비스
 * - 양방향: AES-256-GCM (이름, 전화번호 등 민감정보 암호화)
 * - 단방향: SHA-256 (CI 해시)
 */
@Slf4j
@Component
public class EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    @Value("${encryption.secret}")
    private String secretKey;

    private SecretKeySpec key;

    @PostConstruct
    public void init() {
        try {
            // 서버 기동 시 비밀키를 32바이트(256비트) AES 암호화 열쇠로 초기화
            byte[] keyBytes = digestTo256Bits(secretKey);
            this.key = new SecretKeySpec(keyBytes, "AES");
            log.info("[암호화 서비스] AES-256 키 초기화 완료");
        } catch (Exception e) {
            log.error("[암호화 서비스] 초기화 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.ENCRYPTION_FAILED);
        }
    }

    /**
     * AES-256-GCM 양방향 암호화
     * @param plainText 암호화할 평문
     * @return Base64 인코딩된 암호문 (IV 포함)
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return plainText;
        }

        try {
            byte[] iv = new byte[IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] encryptedWithIv = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, encryptedWithIv, 0, iv.length);
            System.arraycopy(cipherText, 0, encryptedWithIv, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(encryptedWithIv);
        } catch (Exception e) {
            log.error("[암호화 실패] 데이터 암호화 중 오류 발생: {}", e.getMessage());
            throw new CustomException(ErrorCode.ENCRYPTION_FAILED);
        }
    }

    /**
     * AES-256-GCM 복호화
     * @param encryptedText Base64 인코딩된 암호문
     * @return 복호화된 평문
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isBlank()) {
            return encryptedText;
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedText);

            byte[] iv = new byte[IV_LENGTH];
            byte[] cipherText = new byte[decoded.length - IV_LENGTH];
            System.arraycopy(decoded, 0, iv, 0, IV_LENGTH);
            System.arraycopy(decoded, IV_LENGTH, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);

            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("[복호화 실패] 암호문 복호화 중 오류 발생: {}", e.getMessage());
            throw new CustomException(ErrorCode.DECRYPTION_FAILED);
        }
    }

    /**
     * SHA-256 단방향 해시
     * CI 등 변경되지 않아야 하는 식별자 해시에 사용
     * @param plainText 해시할 평문
     * @return Base64 인코딩된 해시값
     */
    public String generateHash(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return plainText;
        }
        return Base64.getEncoder().encodeToString(digestTo256Bits(plainText));
    }

    private byte[] digestTo256Bits(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("[해시 실패] SHA-256 처리 중 오류 발생: {}", e.getMessage());
            throw new CustomException(ErrorCode.ENCRYPTION_FAILED);
        }
    }
}