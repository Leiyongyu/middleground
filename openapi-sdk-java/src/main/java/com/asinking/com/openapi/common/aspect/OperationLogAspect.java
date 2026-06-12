package com.asinking.com.openapi.common.aspect;

import com.alibaba.fastjson2.JSON;
import com.asinking.com.openapi.common.annotation.OperationLog;
import com.asinking.com.openapi.common.response.Result;
import com.asinking.com.openapi.service.OperationLogService;
import com.asinking.com.openapi.utils.JwtTokenService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Spring AOP 切面：拦截 @OperationLog 注解的方法，自动记录操作日志。
 *
 * 解析返回值中的 inserted/updated/skipped/total 等计数，
 * 以及从请求中提取操作人、IP、路径等信息。
 */
@Aspect
@Component
public class OperationLogAspect {

    private static final Logger LOG = LoggerFactory.getLogger(OperationLogAspect.class);
    private final OperationLogService logService;
    private final JwtTokenService jwtTokenService;

    public OperationLogAspect(OperationLogService logService, JwtTokenService jwtTokenService) {
        this.logService = logService;
        this.jwtTokenService = jwtTokenService;
    }

    @Around("@annotation(com.asinking.com.openapi.common.annotation.OperationLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        OperationLog anno = method.getAnnotation(OperationLog.class);

        String apiPath = "";
        String httpMethod = "";
        String operator = "匿名";
        String ipAddress = "";

        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                apiPath = req.getRequestURI();
                httpMethod = req.getMethod();
                ipAddress = getClientIp(req);
                // 直接从 Authorization header 解析用户，不依赖 JWT 拦截器
                String auth = req.getHeader("Authorization");
                if (auth != null && auth.startsWith("Bearer ")) {
                    try { operator = jwtTokenService.parse(auth.substring(7)).getPayload().getSubject(); }
                    catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();

            // 解析返回值中的计数和详情
            int total = 0, success = 0, fail = 0;
            String details = null;
            try {
                Object data = extractData(result);
                if (data instanceof Map) {
                    Map<?, ?> m = (Map<?, ?>) data;
                    total = intVal(m, "total", intVal(m, "processed", 0));
                    success = intVal(m, "inserted", 0) + intVal(m, "updated", 0);
                    fail = intVal(m, "skipped", 0);
                    if (success == 0) success = intVal(m, "success_count", 0);
                    // 提取详细日志
                    Object skipDetails = m.get("skipDetails");
                    if (skipDetails != null) {
                        Map<String, Object> detailMap = new LinkedHashMap<>();
                        detailMap.put("total", total);
                        detailMap.put("inserted", intVal(m, "inserted", 0));
                        detailMap.put("updated", intVal(m, "updated", 0));
                        detailMap.put("skipped", fail);
                        detailMap.put("skipDetails", skipDetails);
                        details = JSON.toJSONString(detailMap);
                    }
                }
            } catch (Exception ignored) {}

            String status = fail > 0 ? "成功(有跳过)" : "成功";
            String target = anno.target() != null && !anno.target().isEmpty() ? anno.target() : apiPath;
            logService.logAsync(apiPath, httpMethod, operator, ipAddress,
                    anno.value(), target, status, total, success, fail, null, details);
            LOG.info("[AOP] {} {} {} -> success({}) fail({}) {}ms",
                    httpMethod, apiPath, anno.target(), success, fail, System.currentTimeMillis() - start);

            return result;
        } catch (Exception e) {
            String target = anno.target() != null && !anno.target().isEmpty() ? anno.target() : apiPath;
            Map<String, Object> errDetail = new LinkedHashMap<>();
            errDetail.put("error", e.getMessage());
            errDetail.put("errorType", e.getClass().getSimpleName());
            logService.logAsync(apiPath, httpMethod, operator, ipAddress,
                    anno.value(), target, "失败", 0, 0, 0, e.getMessage(), JSON.toJSONString(errDetail));
            LOG.error("[AOP] {} {} {} -> FAIL: {} ({}ms)",
                    httpMethod, apiPath, anno.target(), e.getMessage(), System.currentTimeMillis() - start);
            throw e;
        }
    }

    /** 从 Result 对象中提取 data */
    private Object extractData(Object result) {
        if (result instanceof Result) {
            return ((Result<?>) result).getData();
        }
        return result;
    }

    private int intVal(Map<?, ?> m, String key, int def) {
        Object v = m.get(key);
        if (v instanceof Number) return ((Number) v).intValue();
        if (v != null) {
            try { return Integer.parseInt(v.toString()); } catch (Exception ignored) {}
        }
        return def;
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) return xff.split(",")[0].trim();
        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isEmpty()) return xri;
        return request.getRemoteAddr();
    }
}
