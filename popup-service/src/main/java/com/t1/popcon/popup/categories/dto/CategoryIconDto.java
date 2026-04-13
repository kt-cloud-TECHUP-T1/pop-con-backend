package com.t1.popcon.popup.categories.dto;

import com.t1.popcon.popup.dto.card.PopupCardDto;

// 카테고리 아이콘 DTO
public record CategoryIconDto(
    String iconUrl,
    String iconName,
    Long popupId,
    PopupCardDto.PhaseDto phase
) {}
