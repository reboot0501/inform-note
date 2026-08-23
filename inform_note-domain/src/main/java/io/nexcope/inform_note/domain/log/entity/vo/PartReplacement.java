package io.nexcope.inform_note.domain.log.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartReplacement {
    //
    private ReplacementType replacementType;
    private String partNo;
    private String partName;
    private Integer qty;
}
