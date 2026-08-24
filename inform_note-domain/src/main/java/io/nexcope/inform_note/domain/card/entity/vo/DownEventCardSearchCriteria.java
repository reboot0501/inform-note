package io.nexcope.inform_note.domain.card.entity.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DownEventCardSearchCriteria {
    // [1행 검색 조건]
    private FabricationPlant fabricationPlant; // FAB/Site
    private ProcessModule processModule;       // Process Module
    private EquipmentModel equipmentModel;     // Eqp Model
    private String equipmentId;                // Eqp ID
    private String keyword;                    // 검색어 (downCode, downCodeDescription, alarmId 등)
    private Boolean isCritical;                // Critical Only

    // [2행 검색 조건]
    private DownType downType;                 // Down Type
    private WorkStatus workStatus;             // Work Status
    private Shift shift;                       // Shift (A, B, C조)
    private String technician;                 // 담당 작업자 (이름 또는 사번)
    private Long downStartDatetimeFrom;        // 시작 일시 (Epoch ms)
    private Long downStartDatetimeTo;          // 종료 일시 (Epoch ms)

    // [정렬]
    private String sortBy;                     // 정렬 기준
    private String sortDirection;              // ASC / DESC

    // [페이징 파라미터 (page/size 방식 및 offset/limit 방식 모두 지원)]
    private Integer page;
    private Integer size;
    private Integer offset;
    private Integer limit;

    public int getOffset() {
        if (this.offset != null && this.offset >= 0) {
            return this.offset;
        }
        int targetPage = (this.page != null && this.page > 0) ? this.page : 1;
        int targetSize = (this.size != null && this.size > 0) ? this.size : 20;
        return (targetPage - 1) * targetSize;
    }

    public int getLimit() {
        if (this.limit != null && this.limit > 0) {
            return this.limit;
        }
        return (this.size != null && this.size > 0) ? this.size : 20;
    }
}
