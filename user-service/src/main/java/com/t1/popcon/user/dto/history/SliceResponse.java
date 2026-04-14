package com.t1.popcon.user.dto.history;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SliceResponse<T> {

    private List<T> content;
    private boolean first;
    private boolean last;
    private int numberOfElements;
    private boolean empty;
}
