package io.nexcope.inform_note.domain.employees.entity.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmployeeSearchCriteria {
    // name 패턴 검색 조건
    private String namePattern;
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
