package io.nexcope.inform_note.domain.file.entity.vo;

import io.nexcope.inform_note.base.domain.entity.CodeName;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
@AllArgsConstructor
public enum FileStatus {
    //
    TEMP("임시"),
    SAVED("저장됨"),
    DELETED("삭제됨");

    private final String description;

    public static List<String> getDescriptions() {
        return Arrays.stream(values())
                .map(FileStatus::getDescription)
                .toList();
    }

    public static List<CodeName> getCodeNames() {
        return Arrays.stream(values())
                .map(status -> CodeName.of(status.name(), status.getDescription()))
                .toList();
    }
}
