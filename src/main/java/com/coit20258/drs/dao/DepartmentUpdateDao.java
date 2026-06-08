package com.coit20258.drs.dao;

import java.util.List;
import com.coit20258.drs.model.DepartmentUpdate;

public interface DepartmentUpdateDao {
    DepartmentUpdate save(DepartmentUpdate update);
    List<DepartmentUpdate> findByReportId(int reportId);
    List<DepartmentUpdate> findByDepartmentId(int departmentId);
}
