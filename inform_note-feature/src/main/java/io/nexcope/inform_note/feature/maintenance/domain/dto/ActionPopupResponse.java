package io.nexcope.inform_note.feature.maintenance.domain.dto;

import io.nexcope.inform_note.base.util.json.JsonSerializable;
import io.nexcope.inform_note.domain.content.entity.DownContent;
import io.nexcope.inform_note.domain.log.entity.DownEventLog;
import io.nexcope.inform_note.domain.log.entity.vo.*;
import lombok.*;
import org.springframework.beans.BeanUtils;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActionPopupResponse implements JsonSerializable {
    //
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
    private List<PartReplacement> partReplacements;

    // Rich Text 에디터에서 작성한 에디터
    private String contentHtml;    // Froala 에디터 HTML 본문 (Oracle CLOB 매핑)

    public static ActionPopupResponse of(DownEventLog eventLog, DownContent content) {
        //
        ActionPopupResponse response = new ActionPopupResponse();
        BeanUtils.copyProperties(eventLog, response);
        BeanUtils.copyProperties(content, response);

        return response;
    }

}
