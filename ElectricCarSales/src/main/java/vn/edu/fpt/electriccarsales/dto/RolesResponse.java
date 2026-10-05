package vn.edu.fpt.electriccarsales.dto;

import vn.edu.fpt.electriccarsales.entities.Role;

import java.util.List;

public class RolesResponse {
    private List<Role> roles;
    private List<PermissionDTO> permissions;

    public RolesResponse() {
    }

    public RolesResponse(List<Role> roles, List<PermissionDTO> permissions) {
        this.roles = roles;
        this.permissions = permissions;
    }

    public List<Role> getRoles() {
        return roles;
    }

    public void setRoles(List<Role> roles) {
        this.roles = roles;
    }

    public List<PermissionDTO> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<PermissionDTO> permissions) {
        this.permissions = permissions;
    }
}
