package vn.edu.fpt.electriccarsales.dto;

import vn.edu.fpt.electriccarsales.entities.VehicleModel;

import java.util.List;

/**
 * A catalogue recommendation and the product criteria supporting it.
 */
public class VehicleRecommendation {
    private VehicleModel vehicle;
    private List<String> reasons = List.of();

    public VehicleRecommendation() {
    }

    public VehicleRecommendation(VehicleModel vehicle, List<String> reasons) {
        setVehicle(vehicle);
        setReasons(reasons);
    }

    public VehicleModel getVehicle() {
        return vehicle;
    }

    public void setVehicle(VehicleModel vehicle) {
        this.vehicle = vehicle;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public void setReasons(List<String> reasons) {
        this.reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }
}
