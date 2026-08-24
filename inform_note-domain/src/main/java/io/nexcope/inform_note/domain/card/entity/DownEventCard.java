package io.nexcope.inform_note.domain.card.entity;

import io.nexcope.inform_note.base.util.json.JsonUtil;
import io.nexcope.inform_note.domain.card.entity.dto.DownEventCardDto;
import io.nexcope.inform_note.domain.card.entity.vo.*;
import lombok.*;
import org.springframework.beans.BeanUtils;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // MyBatis 리플렉션 매핑을 위한 기본 생성자
@AllArgsConstructor
@Builder
public class DownEventCard {
    // [식별자]
    private String downEventId;
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
    private Long downEndDatetime;
    private Long downDurationMinutes;

    private boolean isCritical;

    // [다운 코드 및 알람]
    private String downCode;
    private String downCodeDescription;
    private String alarmId;

    // [Native JSON 매핑 VO 객체 및 리스트]
    private AssignedTechnician assignedTechnician;
    private Approver approver;
    @Builder.Default
    private List<PartReplacement> partReplacements = new ArrayList<>();

    // [감사 메타]
    private String createdBy;
    private OffsetDateTime createdAt;
    private String updatedBy;
    private OffsetDateTime updatedAt;

    public DownEventCard(DownEventCardDto dto) {
        //
        this.downEventId = dto.genId();
        BeanUtils.copyProperties(dto, this);
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public static String genId(String equipmentId) {
        //
        OffsetDateTime now = OffsetDateTime.now();
        return String.format("%s_D%s", equipmentId, now.format(DateTimeFormatter.ofPattern("yy-MM-dd_HH:mm:ss.SSS")));
    }

    public static DownEventCard fromJson(String json) {
        //
        return JsonUtil.fromJson(json, DownEventCard.class);
    }

    // =================================================================
    // [비즈니스 메서드 (도메인 행위)]
    // =================================================================
    public void changeWorkStatus(WorkStatus newStatus, String modifierId) {
        //
        this.workStatus = newStatus;
        this.updatedBy = modifierId;
    }

    public void completeAction(Long endEpochMs, String modifierId) {
        //
        this.downEndDatetime = endEpochMs;
        this.workStatus = WorkStatus.ACTION_DONE;
        this.updatedBy = modifierId;

        if (this.downStartDatetime != null && endEpochMs != null) {
            //
            long durationMs = endEpochMs - this.downStartDatetime;
            this.downDurationMinutes = Math.max(0, durationMs / (1000 * 60));
        }
    }

    public void assignTechnician(AssignedTechnician technician, String modifierId) {
        //
        this.assignedTechnician = technician;
        this.workStatus = WorkStatus.IN_PROGRESS;
        this.updatedBy = modifierId;
    }

    public void specifyReplacement(List<PartReplacement> partReplacements, String modifierId) {
        //
        this.partReplacements = partReplacements;
        this.updatedBy = modifierId;
    }



    public static void main(String[] args) {
        //
        System.out.println(genId("Example Key"));
    }

}
