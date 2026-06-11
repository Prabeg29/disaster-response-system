package com.coit20258.drs.dao;

import java.util.List;
import java.util.Optional;

import com.coit20258.drs.model.EvacuationZone;

public interface EvacuationZoneDao {

    EvacuationZone create(EvacuationZone zone);

    List<EvacuationZone> findAll();

    List<EvacuationZone> findByReportId(int reportId);

    Optional<EvacuationZone> findById(int id);

    boolean update(EvacuationZone zone);

    boolean updateOccupancy(int id, int currentOccupancy, String status);

    boolean delete(int id);
}
