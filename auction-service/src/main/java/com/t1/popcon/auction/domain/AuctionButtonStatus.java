package com.t1.popcon.auction.domain;

public enum AuctionButtonStatus {
    WAITING,   // 오픈 전
    ENABLED,   // 참여 가능
    SOLD_OUT,  // 수량 없음 / 낙찰 완료
    ENDED      // 종료됨
}
