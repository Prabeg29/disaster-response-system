package com.coit20258.drs.dao;

import java.util.List;
import java.util.Optional;

import com.coit20258.drs.model.DisasterAssessment;

public interface DisasterAssessmentDao {

    DisasterAssessment save(DisasterAssessment assessment);

    boolean update(DisasterAssessment assessment);

    Optional<DisasterAssessment> findByReportId(int reportId);

    Optional<DisasterAssessment> findById(int assessmentId);

    List<DisasterAssessment> findAll();
}
