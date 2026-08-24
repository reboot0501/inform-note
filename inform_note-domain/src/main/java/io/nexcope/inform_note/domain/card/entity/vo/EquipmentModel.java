package io.nexcope.inform_note.domain.card.entity.vo;

import io.nexcope.inform_note.base.domain.entity.CodeName;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
@AllArgsConstructor
public enum EquipmentModel {
    // Photo
    ASML_NXT_1980DI("ASML NXT:1980DI"),

    ASML_NXT_1980Di("ASML NXT:1980Di"),
    NIKON_NSR_S620D("NIKON NSR-S620D"),
    ASML_TWINSCAN("ASML TWINSCAN"),
    CANON_FPA_6300ES6A("CANON FPA-6300ES6A"),
    TEL_LITHIUS_PRO_i("TEL Lithius Pro i"),

    // Etch
    LAM_KIYO_CX("LAM KIYO CX"),
    TEL_TACTRAS("TEL TACTRAS"),
    AMAT_CENTRIS_SYM3("AMAT CENTRIS SYM3"),
    LAM_VERSYS("LAM VERSYS"),

    // CVD / Thin Film
    TEL_TRIAS("TEL TRIAS"),
    AMAT_PRODUCER("AMAT PRODUCER"),
    ASM_EAGLE_12("ASM EAGLE 12"),
    JUSUNG_EUREKA("JUSUNG EUREKA"),
    AMAT_ENDURE_CLOVER("AMAT Endura Clover"),

    // CMP
    AMAT_REFLEXION("AMAT REFLEXION"),
    EBARA_F_REX("EBARA F-REX"),
    KC_TECH_CMP_300("KC TECH CMP-300"),

    // Diffusion
    KOKUSAI_ADVANCED("KOKUSAI ADVANCED"),
    TEL_ALPHA("TEL ALPHA"),
    ASM_A412("ASM A412"),

    // Implant
    VARIAN_VIISTA("VARIAN VIISTA"),
    AXCELIS_PURION_H("AXCELIS PURION H"),
    AMAT_VIISTA_TRIDENT("AMAT VIISTA TRIDENT"),

    // Cleans / Strip
    SEMES_IRIS("SEMES IRIS"),
    DNS_SS_3000("DNS SS-3000"),
    TEL_CELLESSE("TEL CELLESSE"),
    PSK_SUPRA("PSK SUPRA"),

    // Metrology / Inspection
    KLA_2925("KLA 2925");


    private final String description;

    public static List<String> getDescriptions() {
        //
        return Arrays.stream(values())
                .map(EquipmentModel::getDescription)
                .toList();
    }

    public static List<CodeName> getCodeNames() {
        //
        return Arrays.stream(values())
                .map(model -> CodeName.of(model.name(), model.getDescription()))
                .toList();
    }
}
