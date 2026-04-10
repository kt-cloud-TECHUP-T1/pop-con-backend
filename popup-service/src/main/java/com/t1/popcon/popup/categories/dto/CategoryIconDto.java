package com.t1.popcon.popup.categories.dto;

// 카테고리 아이콘 DTO - 아이콘 이미지, 이름, 연결된 팝업 ID만 포함
public record CategoryIconDto(
    String iconUrl,
    String iconName,
    Long popupId
) {}
