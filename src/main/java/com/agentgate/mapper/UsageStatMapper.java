package com.agentgate.mapper;

import com.agentgate.entity.UsageStat;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
/**
 * 教学说明：按 appId/apiId/statDate 联合唯一键 INSERT ... ON DUPLICATE KEY UPDATE，原子累加 call_count/error_count，避免 Java 读改写丢计数。
 * 文件路径：src/main/java/com/agentgate/mapper/UsageStatMapper.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */

public interface UsageStatMapper extends BaseMapper<UsageStat> {

    @Insert("INSERT INTO tb_usage_stat(app_id, api_id, stat_date, call_count, error_count, create_time, update_time) " +
            "VALUES(#{appId}, #{apiId}, #{statDate}, 1, #{isError}, NOW(), NOW()) " +
            "ON DUPLICATE KEY UPDATE call_count = call_count + 1, " +
            "error_count = error_count + #{isError}, update_time = NOW()")
    /**
     * 依赖应用/API/日期联合唯一键插入或原子累加日用量，避免 Java 读改写丢失增量。
     */
    int upsertIncrement(@Param("appId") Long appId,
                        @Param("apiId") Long apiId,
                        @Param("statDate") LocalDate statDate,
                        @Param("isError") int isError);
}
