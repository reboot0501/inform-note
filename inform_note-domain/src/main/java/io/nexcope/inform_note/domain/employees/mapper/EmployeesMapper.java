package io.nexcope.inform_note.domain.employees.mapper;

import io.nexcope.inform_note.base.domain.entity.OffsetElementList;
import io.nexcope.inform_note.domain.employees.entity.Employees;
import io.nexcope.inform_note.domain.employees.entity.vo.EmployeeSearchCriteria;
import io.nexcope.inform_note.domain.employees.entity.vo.ExtractEmployee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface EmployeesMapper {

    // 단건 조회 (PK 기준)
    Optional<Employees> findById(@Param("employeeId") int employeeId);

    // 전체 목록 조회
    List<Employees> findAll();

    // 부서별 목록 조회
    List<Employees> findByDepartmentId(@Param("departmentId") Long departmentId);

    // 등록 (Insert)
    int insert(Employees employees);

    // 수정 (Update)
    int update(Employees employees);

    // 삭제 (Delete)
    int deleteById(@Param("employeeId") Long employeeId);

    // 단건 조회 (empNo 기준)
    Optional<Employees> findByEmpNo(@Param("empNo") String empNo);

    // 정규식 이름 패턴 기반 직원 추출 페이징 목록 조회
    List<ExtractEmployee> findByNamePattern(EmployeeSearchCriteria criteria);

    // 정규식 이름 패턴 기반 직원 추출 전체 건수 조회
    long countByNamePattern(EmployeeSearchCriteria criteria);
}
