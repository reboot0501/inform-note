package io.nexcope.inform_note.domain.card.entity.vo;

import io.nexcope.inform_note.base.domain.entity.CodeName;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
@AllArgsConstructor
public enum FabricationPlant {
    //
    FAB_1("FAB-1 (300mm)"),
    FAB_2("FAB-2 (300mm)"),
    FAB_3("FAB-3 (300mm)"),
    RND_FAB("R&D FAB"),
    PKG("PKG Line");

    private final String description;

    public static List<String> getDescriptions() {
        //
        return Arrays.stream(values())
                .map(FabricationPlant::getDescription)
                .toList();
    }

    public static List<CodeName> getCodeNames() {
        //
        return Arrays.stream(values())
                .map(fab -> CodeName.of(fab.name(), fab.getDescription()))
                .toList();
    }

}
