package io.nexcope.inform_note.domain.file.entity.vo;

import io.nexcope.inform_note.base.domain.entity.CodeName;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
@AllArgsConstructor
public enum FileRefType {
    //
    DOWN_CONTENT_INLINE("다운조치본문인라인"),
    DOWN_ATTACHMENT("다운이벤트일반첨부"),
    ETC("기타");

    private final String description;

    public static List<String> getDescriptions() {
        return Arrays.stream(values())
                .map(FileRefType::getDescription)
                .toList();
    }

    public static List<CodeName> getCodeNames() {
        return Arrays.stream(values())
                .map(type -> CodeName.of(type.name(), type.getDescription()))
                .toList();
    }
}
