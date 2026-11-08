package com.electriccarsales.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AdminRole implements Serializable {
    private static final long serialVersionUID = 1L;

    private int roleId;
    private String roleName;
    private List<String> permissions = new ArrayList<>();

    public AdminRole() {
    }

    public AdminRole(int roleId, String roleName) {
        this.roleId = roleId;
        this.roleName = roleName;
    }

    public AdminRole(int roleId, String roleName, List<String> permissions) {
        this.roleId = roleId;
        this.roleName = roleName;
        this.permissions = permissions != null ? permissions : new ArrayList<>();
    }

    public int getRoleId() {
        return roleId;
    }

    public void setRoleId(int roleId) {
        this.roleId = roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions != null ? permissions : new ArrayList<>();
    }
}
