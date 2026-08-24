package io.nexcope.inform_note.domain.card.entity.vo;

import io.nexcope.inform_note.base.domain.entity.CodeName;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
@AllArgsConstructor
public enum ReplacementType {
    //
    REPLACEMENT_PART("Replacement Part"),
    USE_MATERIAL("USE MATERIAL");

    private final String description;

    public static List<String> getDescriptions() {
        //
        return Arrays.stream(values())
                .map(ReplacementType::getDescription)
                .toList();
    }

    public static List<CodeName> getCodeNames() {
        //
        return Arrays.stream(values())
                .map(replacementType -> CodeName.of(replacementType.name(), replacementType.getDescription()))
                .toList();
    }
}
