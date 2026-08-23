package io.nexcope.inform_note.domain.employees.entity.vo;

import io.nexcope.inform_note.base.util.json.JsonSerializable;
import io.nexcope.inform_note.base.util.json.JsonUtil;
import io.nexcope.inform_note.domain.log.entity.vo.Shift;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtractEmployee implements JsonSerializable {
    //
    private String empNo;
    private String name;
    private String departmentId;
    private String departmentName;
    private String jobId;
    private String jobTitle;
    private Shift shift;

    @Override
    public String toString() {
        return toJson();
    }

    public static ExtractEmployee fromJson(String json) {
        return JsonUtil.fromJson(json, ExtractEmployee.class);
    }
}
