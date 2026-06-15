package com.asinking.com.openapi.service;

import com.asinking.com.openapi.mapper.mp.AmzInventoryOverviewMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AmazonComputeService {

    private static final Logger LOG = LoggerFactory.getLogger(AmazonComputeService.class);
    private final AmzInventoryOverviewMapper overviewMapper;

    public AmazonComputeService(AmzInventoryOverviewMapper overviewMapper) {
        this.overviewMapper = overviewMapper;
    }

    public void refreshSnapshot() {
        LOG.info("==== Amazon补货快照 开始 ====");
        long t = System.currentTimeMillis();
        overviewMapper.delete(null);
        overviewMapper.insertByListing();
        LOG.info("==== Amazon补货快照 完成: {} 条 耗时{}ms ====", overviewMapper.selectCount(null), System.currentTimeMillis() - t);
    }
}
