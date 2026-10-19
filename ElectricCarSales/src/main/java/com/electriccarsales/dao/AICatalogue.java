package com.electriccarsales.dao;

import com.electriccarsales.model.VehicleModel;

import java.sql.SQLException;
import java.util.List;

/**
 * The assistant can only obtain this public vehicle projection.
 */
@FunctionalInterface
public interface AICatalogue {
    List<VehicleModel> findModels() throws SQLException;
}
