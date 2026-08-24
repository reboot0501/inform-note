package io.nexcope.inform_note.domain.card.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignedTechnician {
    //
    private String empNo;
    private String name;
    private String jobTitle;
    private Shift shift;

    public static AssignedTechnician of(String empNo, String name, String jobTitle, Shift shift) {
        //
        return AssignedTechnician.builder()
                .empNo(empNo) // tb_employees.emp_no
                .name(name) // tb_employees.name
                .jobTitle(jobTitle) // tb_jobs.job_title
                .shift(shift) // "A", "B", "C"
                .build();
    }
}
