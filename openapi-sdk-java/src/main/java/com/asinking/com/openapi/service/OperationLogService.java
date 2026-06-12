package com.asinking.com.openapi.service;

import com.asinking.com.openapi.common.response.PageResult;
import com.asinking.com.openapi.entity.OperationLogEntity;
import com.asinking.com.openapi.mapper.mp.OperationLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class OperationLogService {

    private final OperationLogMapper mapper;

    public OperationLogService(OperationLogMapper mapper) { this.mapper = mapper; }

    /** AOP 切面使用：异步写入，不阻塞业务响应 */
    @Async
    public void logAsync(String apiPath, String httpMethod, String operator, String ipAddress,
                    String operationType, String target, String status,
                    Integer total, Integer success, Integer fail, String error, String details) {
        logSync(apiPath, httpMethod, operator, ipAddress, operationType, target, status,
                total, success, fail, error, details);
    }

    public void log(String apiPath, String httpMethod, String operator, String ipAddress,
                    String operationType, String target, String status,
                    Integer total, Integer success, Integer fail, String error) {
        logSync(apiPath, httpMethod, operator, ipAddress, operationType, target, status,
                total, success, fail, error, null);
    }

    /** 写入含详细JSON的完整日志 */
    public void log(String apiPath, String httpMethod, String operator, String ipAddress,
                    String operationType, String target, String status,
                    Integer total, Integer success, Integer fail, String error, String details) {
        logSync(apiPath, httpMethod, operator, ipAddress, operationType, target, status,
                total, success, fail, error, details);
    }

    private void logSync(String apiPath, String httpMethod, String operator, String ipAddress,
                    String operationType, String target, String status,
                    Integer total, Integer success, Integer fail, String error, String details) {
        OperationLogEntity e = new OperationLogEntity();
        e.setApiPath(apiPath);
        e.setHttpMethod(httpMethod);
        e.setOperator(operator);
        e.setIpAddress(ipAddress);
        e.setOperationType(operationType);
        e.setTarget(target);
        e.setStatus(status);
        e.setTotalCount(total);
        e.setSuccessCount(success);
        e.setFailCount(fail);
        if (error != null && error.length() > 500) error = error.substring(0, 500);
        e.setErrorMessage(error);
        e.setDetails(details);
        e.setCreateTime(LocalDateTime.now());
        mapper.insert(e);
    }

    /** 定时任务使用：无HTTP信息 */
    public void log(String operationType, String target, String status,
                    Integer total, Integer success, Integer fail, String error) {
        log(null, null, "SYSTEM", null, operationType, target, status, total, success, fail, error);
    }

    /** 删除15天前的日志 */
    public int cleanOldLogs() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(15);
        return mapper.delete(new LambdaQueryWrapper<OperationLogEntity>()
                .lt(OperationLogEntity::getCreateTime, cutoff));
    }

    /** 分页查询（使用 MyBatis-Plus Page 避免 SQL 拼接） */
    public PageResult<OperationLogEntity> page(long page, long size) {
        long p = page <= 0 ? 1 : page;
        long s = size <= 0 ? 20 : Math.min(size, 100);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<OperationLogEntity> mpPage =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(p, s);
        mpPage = mapper.selectPage(mpPage,
                new LambdaQueryWrapper<OperationLogEntity>()
                        .orderByDesc(OperationLogEntity::getId));
        return new PageResult<>(mpPage.getTotal(), mpPage.getCurrent(), mpPage.getSize(), mpPage.getRecords());
    }
}
