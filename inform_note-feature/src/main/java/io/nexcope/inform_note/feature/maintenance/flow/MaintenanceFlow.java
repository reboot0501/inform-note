package io.nexcope.inform_note.feature.maintenance.flow;

import io.nexcope.inform_note.base.util.json.JsonUtil;
import io.nexcope.inform_note.domain.content.entity.DownContent;
import io.nexcope.inform_note.domain.content.entity.dto.DownContentDto;
import io.nexcope.inform_note.domain.content.logic.DownContentLogic;
import io.nexcope.inform_note.domain.file.logic.AttachedFileLogic;
import io.nexcope.inform_note.domain.log.entity.DownEventLog;
import io.nexcope.inform_note.domain.log.entity.vo.AssignedTechnician;
import io.nexcope.inform_note.domain.log.entity.vo.PartReplacement;
import io.nexcope.inform_note.domain.log.entity.vo.WorkStatus;
import io.nexcope.inform_note.domain.log.logic.DownEventLogLogic;
import io.nexcope.inform_note.feature.file_handler.task.FileHandlerUtils;
import io.nexcope.inform_note.feature.maintenance.task.MaintenanceUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
public class MaintenanceFlow {
    //
    private final DownEventLogLogic downEventLogLogic;
    private final DownContentLogic downContentLogic;
    private final AttachedFileLogic attachedFileLogic;

    public String correctiveAction(String downEventId, AssignedTechnician newTechnician,
                                   List<PartReplacement> newParts, String contentHtml, WorkStatus workStatus) {
        //
        DownEventLog downEventLog = downEventLogLogic.findById(downEventId);
        if(downEventLog == null) throw new NoSuchElementException(downEventId + "가  존재 하지 않습니다.!!1");
        String empNo = newTechnician.getEmpNo();
        // Down Event Log 유지보수 진헹 처리 -- 수정
        if(WorkStatus.ACTION_DONE.equals(workStatus)) {
            //
            downEventLog.completeAction(OffsetDateTime.now().toInstant().toEpochMilli(), empNo);
        } else {
            // WorkStatus.IN_PROGRESS, WorkStatus.VERIFIED, WorkStatus.CLOSED
            downEventLog.changeWorkStatus(workStatus, empNo);
        }
        if(MaintenanceUtils.isEqualAssignTechnician(newTechnician, downEventLog.getAssignedTechnician())) {
            //
            downEventLog.assignTechnician(newTechnician, empNo);
        }
        if(MaintenanceUtils.isEqualPartReplacements(newParts, downEventLog.getPartReplacements())) {
            //
            downEventLog.specifyReplacement(newParts, empNo);
        }
        // Down Event Log 수정
        downEventLogLogic.modify(downEventLog);
        // Down Content 저장
        this.saveContent(downEventId, contentHtml, empNo);
        Set<String> htmlToFileKeys = FileHandlerUtils.extractFileKeysFromHtml(contentHtml);
        // File 저장 처리
        if(!htmlToFileKeys.isEmpty()) attachedFileLogic.saveFiles(htmlToFileKeys, downEventId, empNo);
        return downEventLog.getDownEventId();
    }

    private void saveContent(String downEventId, String contentHtml, String empNo) {
        //
        DownContent downContent = downContentLogic.findById(downEventId);
        if( contentHtml != null ) {
            if (downContent == null) {
                // Down Content 등록
                DownContentDto downContentDto = DownContentDto.builder()
                        .downEventId(downEventId)
                        .contentHtml(contentHtml)
                        .build();
                // Down Content 등록
                downContentLogic.register(downContentDto);
            } else {
                // Down Content 수정
                downContent.fromNewContent(contentHtml, empNo);
                downContentLogic.modify(downContent);
            }
        }
    }



}

