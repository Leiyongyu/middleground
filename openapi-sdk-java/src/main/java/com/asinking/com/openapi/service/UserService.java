package com.asinking.com.openapi.service;

import com.asinking.com.openapi.dto.response.UserLoginResponse;
import com.asinking.com.openapi.common.response.PageResult;
import com.asinking.com.openapi.dto.response.UserResponse;
import com.asinking.com.openapi.entity.UserEntity;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 用户业务接口，管理用户认证和 CRUD。
 */
public interface UserService extends IService<UserEntity> {

    UserLoginResponse login(String account, String password);

    void logout(String authorizationHeader);

    UserResponse createUser(String operatorUserId, String account, String password, String role, String ownerName, String brandCode);

    UserResponse updateUser(String operatorUserId, String id, String role, String ownerName, String password);

    boolean deleteUser(String operatorUserId, String id);

    UserResponse getUserById(String id);

    Page<UserEntity> pageUsers(long page, long size, String account, String role);

    PageResult<UserResponse> pageUserResponses(long page, long size, String account, String role);
}
