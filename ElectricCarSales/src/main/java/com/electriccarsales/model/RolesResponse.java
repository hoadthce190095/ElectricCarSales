package com.electriccarsales.model;

import java.util.List;

public class RolesResponse {
    private List<AdminRole> roles;
    private List<PermissionDTO> permissions;

    public RolesResponse() {
    }

    public RolesResponse(List<AdminRole> roles, List<PermissionDTO> permissions) {
        this.roles = roles;
        this.permissions = permissions;
    }

    public List<AdminRole> getRoles() {
        return roles;
    }

    public void setRoles(List<AdminRole> roles) {
        this.roles = roles;
    }

    public List<PermissionDTO> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<PermissionDTO> permissions) {
        this.permissions = permissions;
    }
}
