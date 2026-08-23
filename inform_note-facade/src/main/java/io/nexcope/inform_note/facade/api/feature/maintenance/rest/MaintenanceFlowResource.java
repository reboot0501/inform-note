package io.nexcope.inform_note.facade.api.feature.maintenance.rest;

import io.nexcope.inform_note.domain.log.entity.vo.AssignedTechnician;
import io.nexcope.inform_note.domain.log.entity.vo.PartReplacement;
import io.nexcope.inform_note.domain.log.entity.vo.WorkStatus;
import io.nexcope.inform_note.facade.api.feature.maintenance.command.CorrectiveActionCommand;
import io.nexcope.inform_note.feature.maintenance.flow.MaintenanceFlow;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Maintenance", description = "정비/보수 저장 관련 API")
@RestController
@RequestMapping("/feature/maintenance")
@RequiredArgsConstructor
public class MaintenanceFlowResource {
    //
    private final MaintenanceFlow maintenanceFlow;

    @Operation(summary = "정비 작업(Troubleshooting) 처리", description = "장비 고장 이벤트에 대한 정비/보수 작업을 처리합니다.")
    @PostMapping("/corrective-action/command")
    public String correctiveAction(@RequestBody CorrectiveActionCommand command) {
        //
        command.validate();
        String downEventId = command.getDownEventId();
        AssignedTechnician assignedTechnician = command.getAssignedTechnician();
        List<PartReplacement> partReplacements = command.getPartReplacements();
        String contentHtml = command.getContentHtml();
        WorkStatus workStatus = command.getWorkStatus();
        return maintenanceFlow.correctiveAction(downEventId, assignedTechnician, partReplacements, contentHtml, workStatus);
    }
}
