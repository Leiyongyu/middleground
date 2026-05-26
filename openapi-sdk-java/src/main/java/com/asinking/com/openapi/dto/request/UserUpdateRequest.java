package com.asinking.com.openapi.dto.request;

public class UserUpdateRequest {

    private String role;
    private String ownerName;
    private String password;

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
