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

    /** 파이썬 서버 명세에 맞춰 session_id를 쿼리 파라미터로 전달 */
    @GetMapping("/api/v1/test/next")
    VqaNextQuestionResponse getNextQuestion(@RequestParam("session_id") Long sessionId);

    @PostMapping("/api/v1/test/submit")
    VqaSubmitResponse submitAnswer(@RequestBody VqaSubmitRequest request);
}
