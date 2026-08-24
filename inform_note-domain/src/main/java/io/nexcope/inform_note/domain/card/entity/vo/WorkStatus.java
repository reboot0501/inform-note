package io.nexcope.inform_note.domain.card.entity.vo;

import io.nexcope.inform_note.base.domain.entity.CodeName;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
@AllArgsConstructor
public enum WorkStatus {
    //
    DOWN_OCCURRED("발생"),
    IN_PROGRESS("조치중"),
    ACTION_DONE("조치완료"),
    VERIFIED("검증완료"),
    CLOSED("종결");

    private final String description;

    public static List<String> getDescriptions() {
        //
        return Arrays.stream(values())
                .map(WorkStatus::getDescription)
                .toList();
    }

    public static List<CodeName> getCodeNames() {
        //
        return Arrays.stream(values())
                .map(workstatus -> CodeName.of(workstatus.name(), workstatus.getDescription()))
                .toList();
    }
}
