package io.nexcope.inform_note.domain.log.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Approver {
    //
    private String empNo;
    private String JobTitle;
    private String name;
    private long approvedAt;
}
