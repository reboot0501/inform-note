package io.nexcope.inform_note.domain.log.entity.dto;

import io.nexcope.inform_note.base.util.json.JsonSerializable;
import io.nexcope.inform_note.base.util.json.JsonUtil;
import io.nexcope.inform_note.domain.log.entity.DownEventLog;
import io.nexcope.inform_note.domain.log.entity.vo.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;

@Slf4j
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DownEventLogDto  implements JsonSerializable {
    //
    private String equipmentId;
    private String chamberId;

    // [Enums]
    private FabricationPlant fabricationPlant;
    private ProcessModule processModule;
    private EquipmentModel equipmentModel;
    private DownType downType;
    private WorkStatus workStatus;

    // [일시 및 소요시간 (Epoch Milliseconds)]
    private Long downStartDatetime;

    // [다운 코드 및 알람]
    private String downCode;
    private String downCodeDescription;
    private String alarmId;

    public String genId() {
        //
        return DownEventLog.genId(equipmentId);
    }

    public static DownEventLogDto fromJson(String json) {
        //
        return JsonUtil.fromJson(json, DownEventLogDto.class);
    }

    public static DownEventLogDto sample() {
        //
        return DownEventLogDto.builder()
                .equipmentId("PH-ASML-01") // 장비 고유 ID
                .chamberId("Ch-A") // 챔버 고유 ID
                .fabricationPlant(FabricationPlant.FAB_1) // FAB 코드 (Enum: FabricationPlant)
                .processModule(ProcessModule.CMP) // 공정 모듈 (Enum: ProcessModule)
                .equipmentModel(EquipmentModel.AMAT_ENDURE_CLOVER) // 장비 모델 (Enum: EquipmentModel)
                .downType(DownType.HARDWARE) // 다운 종류 (Enum: DownType)
                .workStatus(WorkStatus.DOWN_OCCURRED) // 작업 진행 상태 (Enum: WorkStatus)
                .downStartDatetime(OffsetDateTime.now().toInstant().toEpochMilli()) // 다운 시작 일시 (Epoch ms)
                .downCode("HW-VAC-302") // 다운 코드
                .downCodeDescription("Vacuum Leak Detected") // 다운 코드 상세 설명
                .alarmId("ALARM-5021") // 알람 ID
                .build();
    }

    public static void main(String[] args) {
        //
        log.info(sample().toPrettyJson());
    }

}
