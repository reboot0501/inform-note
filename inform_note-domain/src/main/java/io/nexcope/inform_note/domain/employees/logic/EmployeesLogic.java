package io.nexcope.inform_note.domain.employees.logic;

import io.nexcope.inform_note.base.domain.entity.OffsetElementList;
import io.nexcope.inform_note.domain.employees.entity.Employees;
import io.nexcope.inform_note.domain.employees.entity.vo.EmployeeSearchCriteria;
import io.nexcope.inform_note.domain.employees.entity.vo.ExtractEmployee;
import io.nexcope.inform_note.domain.employees.mapper.EmployeesMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class EmployeesLogic {

    private final EmployeesMapper mapper;

    @Transactional(readOnly = true)
    public Employees findById(int employeeId) {
        //
        return mapper.findById(employeeId).orElseGet(null);
    }

    @Transactional(readOnly = true)
    public Employees findByEmpNo(String empNo) {
        //
        return mapper.findByEmpNo(empNo).orElseGet(null);
    }

    @Transactional(readOnly = true)
    public List<Employees> findAll() {
        return mapper.findAll();
    }

    @Transactional(readOnly = true)
    public List<Employees> findByDepartmentId(Long departmentId) {
        //
        return mapper.findByDepartmentId(departmentId);
    }

    public Employees register(Employees employees) {
        //
        mapper.insert(employees);
        return employees;
    }

    public int modify(Employees employees) {
        return mapper.update(employees);
    }

    public int deleteById(Long employeeId) {
        return mapper.deleteById(employeeId);
    }

    @Transactional(readOnly = true)
    public OffsetElementList<ExtractEmployee> findByCriteria(EmployeeSearchCriteria criteria) {
        List<ExtractEmployee> results = mapper.findByNamePattern(criteria);
        long totalCount = mapper.countByNamePattern(criteria);
        int offset = criteria != null ? criteria.getOffset() : 0;
        int limit = criteria != null ? criteria.getLimit() : 20;
        return OffsetElementList.of(results, totalCount, offset, limit);
    }

}
