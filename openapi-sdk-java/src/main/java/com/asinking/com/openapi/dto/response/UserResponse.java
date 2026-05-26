package com.asinking.com.openapi.dto.response;

import java.time.LocalDateTime;

public class UserResponse {

    private String id;
    private String account;
    private String role;
    private String ownerName;
    private java.util.List<String> owners;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getAccount() { return account; }
    public void setAccount(String account) { this.account = account; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public java.util.List<String> getOwners() { return owners; }
    public void setOwners(java.util.List<String> owners) { this.owners = owners; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
