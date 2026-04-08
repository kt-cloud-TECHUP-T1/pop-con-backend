package com.t1.popcon.popup.likes.dto;

import java.util.List;

public record SliceResponse<T>(
    List<T> content,
    boolean first,
    boolean last,
    int numberOfElements,
    boolean empty
) {
    public static <T> SliceResponse<T> from(org.springframework.data.domain.Slice<T> slice) {
        return new SliceResponse<>(
            slice.getContent(),
            slice.isFirst(),
            slice.isLast(),
            slice.getNumberOfElements(),
            slice.isEmpty()
        );
    }
}
