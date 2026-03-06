package com.t1.popcon.popup.dto.section;

import java.util.List;

public record PopupSectionResponse<T>(
    SectionKey sectionKey,
    int itemCount,
    List<T> items
) {
}