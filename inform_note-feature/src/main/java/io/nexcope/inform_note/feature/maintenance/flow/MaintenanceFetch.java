package io.nexcope.inform_note.feature.maintenance.flow;

import io.nexcope.inform_note.base.domain.entity.OffsetElementList;
import io.nexcope.inform_note.base.util.json.JsonUtil;
import io.nexcope.inform_note.domain.card.entity.DownEventCard;
import io.nexcope.inform_note.domain.content.entity.DownContent;
import io.nexcope.inform_note.domain.content.logic.DownContentLogic;
import io.nexcope.inform_note.domain.employees.entity.vo.EmployeeSearchCriteria;
import io.nexcope.inform_note.domain.employees.entity.vo.ExtractEmployee;
import io.nexcope.inform_note.domain.employees.logic.EmployeesLogic;
import io.nexcope.inform_note.domain.card.entity.vo.*;
import io.nexcope.inform_note.domain.card.logic.DownEventCardLogic;
import io.nexcope.inform_note.feature.maintenance.domain.dto.ActionPopupResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MaintenanceFetch {
    //
    private final DownEventCardLogic downEventCardLogic;
    private final DownContentLogic downContentLogic;
    private final EmployeesLogic employeesLogic;

    public OffsetElementList<DownEventCard> findDownEvents(DownEventCardSearchCriteria criteria) {
        //
        log.info("------------ findDownEvents criteria : {}", JsonUtil.toPrettyJson(criteria));
        return downEventCardLogic.findOffsetElementListByCriteria(criteria);
    }

    public OffsetElementList<ExtractEmployee> findTechnicians(EmployeeSearchCriteria criteria) {
        //
        return employeesLogic.findByCriteria(criteria);
    }

    public ActionPopupResponse findActionPopup(String downEventId) {
        //
        log.info("------------ findActionPopup downEventId : {}", downEventId);
        DownEventCard eventLog = downEventCardLogic.findById(downEventId);
        DownContent content = downContentLogic.findById(downEventId);
        log.info("------------ findActionPopup content : {}", JsonUtil.toPrettyJson(content));
        return ActionPopupResponse.of(eventLog, content);
    }
}
