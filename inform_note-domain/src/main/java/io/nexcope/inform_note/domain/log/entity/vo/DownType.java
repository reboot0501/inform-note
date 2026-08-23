package io.nexcope.inform_note.domain.log.entity.vo;

import io.nexcope.inform_note.base.domain.entity.CodeName;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
@AllArgsConstructor
public enum DownType {
    //
    HARDWARE("HARDWARE (하드웨어 고장/결함)"),
    SOFTWARE("SOFTWARE (소프트웨어/통신 오류)"),
    PROCESS("PROCESS (공정 파라미터/품질 이상)"),
    UTILITY("UTILITY (유틸리티/가스/전원 이상)"),
    OPTICAL("OPTICAL (광학계/센서 오차)"),
    CONSUMABLE("CONSUMABLE (소모품/패드/필터 수명 종료)"),
    PREVENTIVE("PREVENTIVE (예방 보전/정기 PM)"),
    OPERATOR("OPERATOR (작업자 실수)");


    private final String description;

    public static List<String> getDescriptions() {
        //
        return Arrays.stream(values())
                .map(DownType::getDescription)
                .toList();
    }

    public static List<CodeName> getCodeNames() {
        //
        return Arrays.stream(values())
                .map(downType -> CodeName.of(downType.name(), downType.getDescription()))
                .toList();
    }
}