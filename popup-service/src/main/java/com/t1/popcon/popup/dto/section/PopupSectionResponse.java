package com.t1.popcon.popup.dto.section;

import com.t1.popcon.popup.dto.card.PopupCardDto;
import java.util.List;

public record PopupSectionResponse<T>(
    SectionKey sectionKey,
    int itemCount,
    List<T> items
) {
}