package com.t1.popcon.auction.domain;

public enum AuctionStatus {
    SCHEDULED, // 오픈 전
    OPEN,      // 진행 중
    SOLD_OUT,  // 낙찰 완료
    CLOSED     // 종료
}
