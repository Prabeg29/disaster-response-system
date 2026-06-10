package com.coit20258.drs.dao;

import java.util.List;

import com.coit20258.drs.model.EvacuationZone;

public interface EvacuationZoneDao {

    EvacuationZone create(EvacuationZone zone);

    List<EvacuationZone> findAll();

    List<EvacuationZone> findByReportId(int reportId);

    boolean updateOccupancy(int id, int currentOccupancy, String status);
}
