package com.t1.popcon.magazine.dto.card;

import com.t1.popcon.magazine.entity.Magazine;

public record MagazineCardDto(
        Long magazineId,
        String title,
        String supportingText,
        String thumbnailUrl
) {
    public static MagazineCardDto from(Magazine magazine) {
        return new MagazineCardDto(
                magazine.getId(),
                magazine.getTitle(),
                magazine.getSupportingText(),
                magazine.getThumbnailUrl()
        );
    }
}
