package io.nexcope.inform_note.domain.jobs.entity;

import io.nexcope.inform_note.base.util.json.JsonUtil;
import lombok.*;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // MyBatis 매핑용 기본 생성자
@AllArgsConstructor
@Builder
public class Jobs {

    private String jobId;
    private String jobTitle;
    private BigDecimal minSalary;
    private BigDecimal maxSalary;

    public static Jobs fromJson(String json) {
        return JsonUtil.fromJson(json, Jobs.class);
    }
}
