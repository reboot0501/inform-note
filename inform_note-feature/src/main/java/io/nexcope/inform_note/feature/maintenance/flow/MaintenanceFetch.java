package io.nexcope.inform_note.feature.maintenance.flow;

import io.nexcope.inform_note.base.domain.entity.OffsetElementList;
import io.nexcope.inform_note.base.util.json.JsonUtil;
import io.nexcope.inform_note.domain.content.entity.DownContent;
import io.nexcope.inform_note.domain.content.logic.DownContentLogic;
import io.nexcope.inform_note.domain.employees.entity.vo.EmployeeSearchCriteria;
import io.nexcope.inform_note.domain.employees.entity.vo.ExtractEmployee;
import io.nexcope.inform_note.domain.employees.logic.EmployeesLogic;
import io.nexcope.inform_note.domain.log.entity.DownEventLog;
import io.nexcope.inform_note.domain.log.entity.vo.*;
import io.nexcope.inform_note.domain.log.logic.DownEventLogLogic;
import io.nexcope.inform_note.feature.maintenance.domain.dto.ActionPopupResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MaintenanceFetch {
    //
    private final DownEventLogLogic downEventLogLogic;
    private final DownContentLogic downContentLogic;
    private final EmployeesLogic employeesLogic;

    public OffsetElementList<DownEventLog> findDownEvents(DownEventLogSearchCriteria criteria) {
        //
        log.info("------------ findDownEvents criteria : {}", JsonUtil.toPrettyJson(criteria));
        return downEventLogLogic.findOffsetElementListByCriteria(criteria);
    }

    public OffsetElementList<ExtractEmployee> findTechnicians(EmployeeSearchCriteria criteria) {
        //
        return employeesLogic.findByCriteria(criteria);
    }

    public ActionPopupResponse findActionPopup(String downEventId) {
        //
        DownEventLog eventLog = downEventLogLogic.findById(downEventId);
        DownContent content = downContentLogic.findById(downEventId);
        return ActionPopupResponse.of(eventLog, content);
    }
}
