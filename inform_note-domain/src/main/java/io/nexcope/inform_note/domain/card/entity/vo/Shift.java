package io.nexcope.inform_note.domain.card.entity.vo;

import io.nexcope.inform_note.base.domain.entity.CodeName;
import io.nexcope.inform_note.base.util.json.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Getter
@AllArgsConstructor
public enum Shift {
    //
    A("A조 (08:00~16:30)"),
    B("B조 (16:00~00:30)"),
    C("C조 (00:00~08:30)");

    private final String description;

    public static List<String> getDescriptions() {
        //
        return Arrays.stream(values())
                .map(Shift::getDescription)
                .toList();
    }

    public static List<CodeName> getCodeNames() {
        //
        return Arrays.stream(values())
                .map(shift -> CodeName.of(shift.name(), shift.getDescription()))
                .toList();
    }

    public static void main(String[] args) {
        //
        JsonUtil.toPrettyJson(Shift.getCodeNames());
        log.info(JsonUtil.toPrettyJson(Shift.getCodeNames()));
    }
}
