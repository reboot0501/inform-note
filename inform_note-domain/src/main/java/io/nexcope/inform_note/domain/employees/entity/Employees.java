package io.nexcope.inform_note.domain.employees.entity;

import io.nexcope.inform_note.base.util.json.JsonUtil;
import io.nexcope.inform_note.domain.log.entity.vo.Shift;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // MyBatis 매핑용 기본 생성자
@AllArgsConstructor
@Builder
public class Employees {

    private int employeeId;
    private String empNo;
    private Shift shift;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private LocalDate hireDate;
    private String jobId;
    private BigDecimal salary;
    private BigDecimal commissionPct;
    private Long managerId;
    private Long departmentId;

    public static Employees fromJson(String json) {
        return JsonUtil.fromJson(json, Employees.class);
    }
}
