package io.nexcope.inform_note.facade.api.feature.maintenance.rest;

import io.nexcope.inform_note.base.domain.entity.OffsetElementList;
import io.nexcope.inform_note.domain.card.entity.DownEventCard;
import io.nexcope.inform_note.domain.employees.entity.vo.EmployeeSearchCriteria;
import io.nexcope.inform_note.domain.employees.entity.vo.ExtractEmployee;
import io.nexcope.inform_note.domain.card.entity.vo.DownEventCardSearchCriteria;
import io.nexcope.inform_note.facade.api.feature.maintenance.fetch.FindActionPopupFetch;
import io.nexcope.inform_note.facade.api.feature.maintenance.fetch.FindDownEventsFetch;
import io.nexcope.inform_note.facade.api.feature.maintenance.fetch.FindTechniciansFetch;
import io.nexcope.inform_note.feature.maintenance.domain.dto.ActionPopupResponse;
import io.nexcope.inform_note.feature.maintenance.flow.MaintenanceFetch;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Maintenance", description = "정비/보수 조회 관련 API")
@RestController
@RequestMapping("/feature/maintenance")
@RequiredArgsConstructor
public class MaintenanceFetchResource {
    //
    private final MaintenanceFetch maintenanceFetch;

    @Operation(summary = "장비 Down 내역 조회", description = "장비 Down 이벤트 내역을 페이징 조회 합니다.")
    @PostMapping("/find-down-events/fetch")
    public OffsetElementList<DownEventCard> findDownEvents(@RequestBody FindDownEventsFetch fetch) {
        //
        fetch.validate();
        DownEventCardSearchCriteria criteria = fetch.getCriteria();
        return maintenanceFetch.findDownEvents(criteria);
    }

    @Operation(summary = "Technician 정보 조회", description = "Technician 할당을 위한 사원정보 조회")
    @PostMapping("/find-technicians/fetch")
    public OffsetElementList<ExtractEmployee> findTechnicians(@RequestBody FindTechniciansFetch fetch) {
        //
        fetch.validate();
        EmployeeSearchCriteria criteria = fetch.getCriteria();
        return maintenanceFetch.findTechnicians(criteria);
    }

    @Operation(summary = "팝업 정보 조회", description = "현상 및 조치 상세 팝업 정보 조회")
    @PostMapping("/find-action-popup/fetch")
    public ActionPopupResponse findActionPopup(@RequestBody FindActionPopupFetch fetch) {
        //
        fetch.validate();
        String downEventId = fetch.getDownEventId();
        return maintenanceFetch.findActionPopup(downEventId);
    }

    
}
