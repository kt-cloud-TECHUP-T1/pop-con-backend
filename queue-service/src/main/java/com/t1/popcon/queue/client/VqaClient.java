package com.t1.popcon.queue.client;

import com.t1.popcon.queue.dto.vqa.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "vqa-client", url = "${vqa.server.url}")
public interface VqaClient {

    @PostMapping("/api/v1/session/start")
    VqaSessionStartResponse startSession(@RequestBody VqaSessionStartRequest request);

    /** 
     * 문제 정보 조회 시 사용자 점수(score)를 함께 전달 
     * - VQA 서버가 점수를 보고 적절한 레벨의 문제를 주거나 면제 여부 결정
     */
    @GetMapping("/api/v1/test/next")
    VqaNextQuestionResponse getNextQuestion(
        @RequestParam("session_id") String sessionId,
        @RequestParam("anti_macro_score") Integer score
    );

    @PostMapping("/api/v1/test/submit")
    VqaSubmitResponse submitAnswer(@RequestBody VqaSubmitRequest request);
}
