package com.asinking.com.openapi.service;

import com.asinking.com.openapi.entity.BrandOwnerEntity;
import com.asinking.com.openapi.entity.UserEntity;
import com.asinking.com.openapi.mapper.mp.UserMapper;
import com.asinking.com.openapi.utils.JwtTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * 统一用户权限服务 — 从 JWT 解析当前用户，加载角色 + 品牌权限。
 * 所有模块注入此服务即可，不再各自重复实现。
 * 用法：
 *   UserPermission perm = permissionService.getCurrentUser(request);
 *   if (!perm.isAdmin && !perm.brands.isEmpty()) {
 *       // 按 perm.brands 过滤数据
 *   }
 */
@Component
public class UserPermissionService {

    private final JwtTokenService jwtTokenService;
    private final UserMapper userMapper;
    private final BrandOwnerService brandOwnerService;

    public UserPermissionService(JwtTokenService jwtTokenService,
                                  UserMapper userMapper,
                                  BrandOwnerService brandOwnerService) {
        this.jwtTokenService = jwtTokenService;
        this.userMapper = userMapper;
        this.brandOwnerService = brandOwnerService;
    }

    private static final Logger LOG = LoggerFactory.getLogger(UserPermissionService.class);

    /** 从 HTTP 请求解析当前用户权限 */
    public UserPermission fromRequest(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            try {
                String account = jwtTokenService.parse(auth.substring(7)).getPayload().getSubject();
                return build(account);
            } catch (Exception e) {
                LOG.warn("JWT 解析失败: ip={}", request.getRemoteAddr());
            }
        }
        return UserPermission.ANONYMOUS;
    }

    /** 根据用户账号构建权限 */
    public UserPermission build(String account) {
        UserEntity u = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getAccount, account));
        if (u == null) return UserPermission.ANONYMOUS;

        boolean isAdmin = u.getRole() != null && u.getRole() == 1;

        Set<String> brands = Collections.emptySet();
        if (!isAdmin) {
            brands = new HashSet<>();
            // 按 user_id 精确匹配（新逻辑），列不存在时回退到 owner_name
            List<BrandOwnerEntity> byUserId = null;
            try {
                byUserId = brandOwnerService.lambdaQuery()
                        .eq(BrandOwnerEntity::getUserId, u.getId()).list();
            } catch (Exception e) {
                // user_id 列可能还未部署 → 回退到 owner_name 匹配
            }
            if (byUserId != null && !byUserId.isEmpty()) {
                for (BrandOwnerEntity bo : byUserId) {
                    if (StringUtils.hasText(bo.getBrandCode()))
                        brands.add(bo.getBrandCode().trim().toUpperCase());
                }
            }
            // 兼容旧数据：user_id 为空或列不存在时用 owner_name 匹配
            if (brands.isEmpty() && StringUtils.hasText(u.getOwnerName())) {
                for (BrandOwnerEntity bo : brandOwnerService.lambdaQuery()
                        .eq(BrandOwnerEntity::getOwnerName, u.getOwnerName().trim()).list()) {
                    if (StringUtils.hasText(bo.getBrandCode()))
                        brands.add(bo.getBrandCode().trim().toUpperCase());
                }
            }
        }

        String roleLabel = isAdmin ? "admin" : "user";
        return new UserPermission(u.getId(), roleLabel, isAdmin, brands, u.getOwnerName());
    }

    // ===== DTO =====

    public static class UserPermission {
        public static final UserPermission ANONYMOUS = new UserPermission("", "anonymous", false, Collections.emptySet());

        public final String userId;
        public final String role;
        public final boolean isAdmin;
        public final Set<String> brands;
        public final String ownerName;

        public UserPermission(String userId, String role, boolean isAdmin, Set<String> brands) {
            this(userId, role, isAdmin, brands, null);
        }

        public UserPermission(String userId, String role, boolean isAdmin, Set<String> brands, String ownerName) {
            this.userId = userId; this.role = role; this.isAdmin = isAdmin;
            this.brands = brands; this.ownerName = ownerName;
        }

        /** 未认证用户（如 JWT 解析失败） */
        public boolean isAnonymous() {
            return "anonymous".equals(role);
        }

        /** 检查 SKU 前缀是否匹配用户品牌。匿名用户拒绝访问。 */
        public boolean canViewSku(String sku) {
            if (isAnonymous()) return false;
            if (isAdmin || brands.isEmpty()) return true;
            if (!StringUtils.hasText(sku)) return false;
            int i = sku.indexOf('-');
            return brands.contains(i > 0 ? sku.substring(0, i).toUpperCase() : sku.toUpperCase());
        }
    }
}
