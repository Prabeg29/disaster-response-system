package com.coit20258.drs.dao;

import java.util.List;
import java.util.Optional;

import com.coit20258.drs.model.Resource;

public interface ResourceDao {

    Resource create(Resource resource);

    List<Resource> findAll();

    Optional<Resource> findById(int id);

    List<Resource> findByType(String resourceType);

    boolean update(Resource resource);

    boolean delete(int id);
}
