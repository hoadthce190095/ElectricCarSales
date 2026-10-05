package vn.edu.fpt.electriccarsales.repositories;

import vn.edu.fpt.electriccarsales.entities.VehicleModel;

import java.sql.SQLException;
import java.util.List;

/**
 * The assistant can only obtain this public vehicle projection.
 */
@FunctionalInterface
public interface AICatalogue {
    List<VehicleModel> findModels() throws SQLException;
}
