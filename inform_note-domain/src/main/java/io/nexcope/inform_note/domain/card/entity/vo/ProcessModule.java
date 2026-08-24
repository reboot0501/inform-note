package io.nexcope.inform_note.domain.card.entity.vo;

import io.nexcope.inform_note.base.domain.entity.CodeName;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
@AllArgsConstructor
public enum ProcessModule {
    //
    PHOTO("PHOTO (노광)"),
    ETCH("ETCH (식각)"),
    DIFF("DIFF (확산)"),
    CMP("CMP (연마)"),
    CVD("CVD (증착)"),
    IMP("IMP (이온주입)"),
    METAL("METAL (배선)");

    private final String description;

    public static List<String> getDescriptions() {
        //
        return Arrays.stream(values())
                .map(ProcessModule::getDescription)
                .toList();
    }

    public static List<CodeName> getCodeNames() {
        //
        return Arrays.stream(values())
                .map(module -> CodeName.of(module.name(), module.getDescription()))
                .toList();
    }
}
