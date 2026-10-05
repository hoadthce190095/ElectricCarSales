package vn.edu.fpt.electriccarsales.dto;

import java.util.List;

public class CreateRoleRequest {
    private String roleName;
    private List<String> permissions;
    private Integer cloneRoleId;

    public CreateRoleRequest() {
    }

    public CreateRoleRequest(String roleName, List<String> permissions, Integer cloneRoleId) {
        this.roleName = roleName;
        this.permissions = permissions;
        this.cloneRoleId = cloneRoleId;
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
        this.permissions = permissions;
    }

    public Integer getCloneRoleId() {
        return cloneRoleId;
    }

    public void setCloneRoleId(Integer cloneRoleId) {
        this.cloneRoleId = cloneRoleId;
    }
}
