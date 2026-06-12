package com.asinking.com.openapi.service.impl;

import com.asinking.com.openapi.common.exception.BusinessException;
import com.asinking.com.openapi.common.response.PageResult;
import com.asinking.com.openapi.common.response.ResultCode;
import com.asinking.com.openapi.config.TokenBlacklist;
import com.asinking.com.openapi.dto.response.UserLoginResponse;
import com.asinking.com.openapi.dto.response.UserResponse;
import com.asinking.com.openapi.entity.BrandOwnerEntity;
import com.asinking.com.openapi.entity.UserEntity;
import com.asinking.com.openapi.mapper.mp.UserMapper;
import com.asinking.com.openapi.service.BrandOwnerService;
import com.asinking.com.openapi.service.UserService;
import com.asinking.com.openapi.utils.JwtTokenService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 用户业务实现，提供登录认证、CRUD 及品牌归属关联。
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, UserEntity> implements UserService {

    private static final Logger LOG = LoggerFactory.getLogger(UserServiceImpl.class);

    private final JwtTokenService jwtTokenService;
    private final TokenBlacklist tokenBlacklist;
    private final BrandOwnerService brandOwnerService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserServiceImpl(JwtTokenService jwtTokenService, TokenBlacklist tokenBlacklist, BrandOwnerService brandOwnerService) {
        this.jwtTokenService = jwtTokenService;
        this.tokenBlacklist = tokenBlacklist;
        this.brandOwnerService = brandOwnerService;
    }

    /** 用户登录，支持 BCrypt 和 MD5 两种密码校验。 */
    @Override
    public UserLoginResponse login(String account, String password) {
        if (!StringUtils.hasText(account) || !StringUtils.hasText(password)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "账号或密码不能为空");
        }

        UserEntity user = lambdaQuery()
                .eq(UserEntity::getAccount, account)
                .last("limit 1")
                .one();
        if (user == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "账号或密码错误");
        }

        if (!matches(password, user.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "账号或密码错误");
        }

        JwtTokenService.TokenInfo tokenInfo = jwtTokenService.issue(user);
        return new UserLoginResponse(tokenInfo.getToken(), tokenInfo.getExpiresAtMillis(), user.getAccount(), user.getRole() != null && user.getRole() == 1 ? "admin" : "user", user.getOwnerName());
    }

    /** 解析 Bearer token 并将其加入黑名单。 */
    @Override
    public void logout(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "Missing token");
        }

        Jws<Claims> jws = jwtTokenService.parse(token);
        Claims claims = jws.getPayload();
        String jti = claims.getId();
        long expMillis = claims.getExpiration() != null ? claims.getExpiration().getTime() : System.currentTimeMillis();
        tokenBlacklist.revoke(jti, expMillis);
    }

    /** 创建新用户，自动创建品牌归属记录（幂等）。 */
    @Override
    @Transactional
    public UserResponse createUser(String operatorUserId, String account, String password, String role, String ownerName) {
        if (!StringUtils.hasText(account)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "account 不能为空");
        }
        if (!StringUtils.hasText(password)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "password 不能为空");
        }
        if (!StringUtils.hasText(role)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "role 不能为空");
        }

        String trimmedOwnerName = StringUtils.hasText(ownerName) ? ownerName.trim() : null;

        boolean exists = lambdaQuery().eq(UserEntity::getAccount, account).exists();
        if (exists) {
            throw new BusinessException(ResultCode.CONFLICT, "账号已存在");
        }

        UserEntity entity = new UserEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setAccount(account.trim());
        entity.setPassword(passwordEncoder.encode(password));
        entity.setRole(parseRole(role));
        entity.setOwnerName(trimmedOwnerName);

        boolean ok = save(entity);
        if (!ok) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "创建用户失败");
        }
        return toResponse(entity);
    }

    /** 按 ID 更新用户角色、负责人或密码。 */
    @Override
    public UserResponse updateUser(String operatorUserId, String id, String role, String ownerName, String password) {
        if (!StringUtils.hasText(id)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "id 不能为空");
        }

        UserEntity entity = getById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户不存在");
        }

        if (StringUtils.hasText(role)) {
            entity.setRole(parseRole(role));
        }
        if (ownerName != null) {
            if (!StringUtils.hasText(ownerName)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "ownerName 不能为空");
            }
            entity.setOwnerName(ownerName.trim());
        }
        if (StringUtils.hasText(password)) {
            entity.setPassword(passwordEncoder.encode(password));
        }

        boolean ok = updateById(entity);
        if (!ok) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "更新用户失败");
        }
        return toResponse(entity);
    }

    /** 按 ID 删除用户。 */
    @Override
    public boolean deleteUser(String operatorUserId, String id) {
        if (!StringUtils.hasText(id)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "id 不能为空");
        }
        return removeById(id);
    }

    /** 按 ID 查询用户。 */
    @Override
    public UserResponse getUserById(String id) {
        if (!StringUtils.hasText(id)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "id 不能为空");
        }
        UserEntity entity = getById(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户不存在");
        }
        return toResponse(entity);
    }

    /** 分页查询用户，支持按账号和角色筛选。 */
    @Override
    public Page<UserEntity> pageUsers(long page, long size, String account, String role) {
        long p = page <= 0 ? 1 : page;
        long s = size <= 0 ? 10 : Math.min(size, 200);
        Page<UserEntity> mpPage = new Page<>(p, s);
        return page(mpPage, lambdaQuery()
                .like(StringUtils.hasText(account), UserEntity::getAccount, account)
                .eq(StringUtils.hasText(role), UserEntity::getRole, role)
                .getWrapper());
    }

    /** 分页查询用户并返回含品牌归属信息的 UserResponse。 */
    @Override
    public PageResult<UserResponse> pageUserResponses(long page, long size, String account, String role) {
        Page<UserEntity> result = pageUsers(page, size, account, role);
        List<UserEntity> records = result.getRecords() == null ? Collections.emptyList() : result.getRecords();
        Map<String, List<String>> brandIndex = buildBrandIndex(records);
        List<UserResponse> responses = new ArrayList<>(records.size());
        for (UserEntity user : records) {
            responses.add(toResponse(user, brandIndex));
        }
        return new PageResult<>(result.getTotal(), result.getCurrent(), result.getSize(), responses);
    }

    /** 将 UserEntity 转为 UserResponse。 */
    private UserResponse toResponse(UserEntity entity) {
        return toResponse(entity, buildBrandIndex(Collections.singletonList(entity)));
    }

    /** 将 UserEntity 转为 UserResponse（携带品牌归属索引，避免 N+1 查询） */
    private UserResponse toResponse(UserEntity entity, Map<String, List<String>> brandIndex) {
        UserResponse resp = new UserResponse();
        resp.setId(entity.getId());
        resp.setAccount(entity.getAccount());
        resp.setRole(entity.getRole() != null && entity.getRole() == 1 ? "admin" : "user");
        resp.setOwnerName(entity.getOwnerName());
        // 从索引直接取品牌列表，避免每个用户一次 N+1 DB 查询
        String key = StringUtils.hasText(entity.getOwnerName()) ? entity.getOwnerName().trim() : "";
        resp.setOwners(brandIndex.getOrDefault(key, Collections.emptyList()));
        resp.setCreateTime(entity.getCreateTime());
        resp.setUpdateTime(entity.getUpdateTime());
        return resp;
    }

    /** 根据用户列表构建 ownerName → brandCode列表 索引（一次查询，无 N+1） */
    private Map<String, List<String>> buildBrandIndex(List<UserEntity> users) {
        Set<String> ownerNames = new HashSet<>();
        for (UserEntity user : users) {
            if (user != null && StringUtils.hasText(user.getOwnerName())) {
                ownerNames.add(user.getOwnerName().trim());
            }
        }
        if (ownerNames.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> nameList = new ArrayList<>(ownerNames);
        List<BrandOwnerEntity> owners = brandOwnerService.lambdaQuery()
                .in(BrandOwnerEntity::getOwnerName, nameList)
                .list();
        Map<String, List<String>> index = new HashMap<>();
        if (owners != null) {
            for (BrandOwnerEntity bo : owners) {
                if (bo != null && StringUtils.hasText(bo.getBrandCode()) && StringUtils.hasText(bo.getOwnerName())) {
                    index.computeIfAbsent(bo.getOwnerName().trim(), k -> new ArrayList<>())
                            .add(bo.getBrandCode());
                }
            }
        }
        return index;
    }

    /** 校验原始密码与存储密码是否匹配（仅支持 BCrypt） */
    private boolean matches(String raw, String stored) {
        if (!StringUtils.hasText(stored)) {
            return false;
        }
        String trimmed = stored.trim();
        if (trimmed.startsWith("$2a$") || trimmed.startsWith("$2b$") || trimmed.startsWith("$2y$")) {
            return passwordEncoder.matches(raw, trimmed);
        }
        // 旧格式密码(MD5/明文)不再接受，引导用户通过管理员重置密码
        LOG.warn("账号 {} 的密码不是 BCrypt 格式，拒绝登录", stored.length() > 4 ? stored.substring(0, 4) + "***" : "***");
        return false;
    }

    /** 从 Authorization 头中提取 Bearer token。 */
    private String extractBearerToken(String authorization) {
        if (!StringUtils.hasText(authorization)) {
            return null;
        }
        String trimmed = authorization.trim();
        if (trimmed.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return trimmed.substring(7).trim();
        }
        return trimmed;
    }

    @Override
    public UserEntity getByAccount(String account) {
        return lambdaQuery().eq(UserEntity::getAccount, account).one();
    }

    private Integer parseRole(String role) {
        if (role == null) return 2;
        String r = role.trim().toLowerCase();
        return ("admin".equals(r) || "1".equals(r)) ? 1 : 2;
    }
}
