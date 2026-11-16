package com.agentgate.mapper;

import com.agentgate.entity.UsageStat;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

public interface UsageStatMapper extends BaseMapper<UsageStat> {

    @Insert("INSERT INTO tb_usage_stat(app_id, api_id, stat_date, call_count, error_count, create_time, update_time) " +
            "VALUES(#{appId}, #{apiId}, #{statDate}, 1, #{isError}, NOW(), NOW()) " +
            "ON DUPLICATE KEY UPDATE call_count = call_count + 1, " +
            "error_count = error_count + #{isError}, update_time = NOW()")
    int upsertIncrement(@Param("appId") Long appId,
                        @Param("apiId") Long apiId,
                        @Param("statDate") LocalDate statDate,
                        @Param("isError") int isError);
}
