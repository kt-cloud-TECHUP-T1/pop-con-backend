package com.t1.popcon.draw.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DrawEntryStatus {
    APPLIED("응모완료"),
    WINNER("당첨"),
    FAILED("미당첨");

    private final String description;
}
