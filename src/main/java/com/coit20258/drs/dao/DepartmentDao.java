package com.coit20258.drs.dao;

import java.util.List;
import java.util.Optional;

import com.coit20258.drs.model.Department;

public interface DepartmentDao {

    /** Returns all active departments, ordered by name. */
    List<Department> findAll();

    Optional<Department> findById(int departmentId);

    Optional<Department> findByName(String name);
}
