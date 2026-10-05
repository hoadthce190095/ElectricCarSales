package vn.edu.fpt.electriccarsales.dto;

import java.util.List;

public class RoleRequest {
    private List<String> permissions;

    public RoleRequest() {
    }

    public RoleRequest(List<String> permissions) {
        this.permissions = permissions;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }
}
