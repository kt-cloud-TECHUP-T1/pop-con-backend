package com.t1.popcon.magazine.dto.section;

import com.t1.popcon.magazine.dto.card.MagazineCardDto;

import java.util.List;

public record MagazineSectionResponse(
        String sectionKey,
        int itemCount,
        List<MagazineCardDto> items
){
    private static final String SECTION_KEY = "MAGAZINES";

    public static MagazineSectionResponse of(List<MagazineCardDto> items) {
        return new MagazineSectionResponse(
                SECTION_KEY,
                items.size(),
                items
        );
    }
}
