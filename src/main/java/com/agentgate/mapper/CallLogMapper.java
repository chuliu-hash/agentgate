package com.agentgate.mapper;

import com.agentgate.entity.CallLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
/**
 * 教学说明：自定义 INSERT IGNORE 保证 trace_id 重复时不再次落库；提供 count/avg/distinct/group by 聚合查询，用参数绑定和可选 appId 条件限制统计范围。
 * 文件路径：src/main/java/com/agentgate/mapper/CallLogMapper.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */

public interface CallLogMapper extends BaseMapper<CallLog> {

    /**
     * 依赖 trace_id 唯一索引做幂等：重复消费返回 0，不抛 DuplicateKeyException。
     */
    @Insert("INSERT IGNORE INTO tb_call_log(trace_id, app_id, api_id, method, path, status_code, " +
            "latency_ms, client_ip, error_code, request_size, response_size, create_time) " +
            "VALUES(#{log.traceId}, #{log.appId}, #{log.apiId}, #{log.method}, #{log.path}, " +
            "#{log.statusCode}, #{log.latencyMs}, #{log.clientIp}, #{log.errorCode}, " +
            "#{log.requestSize}, #{log.responseSize}, #{log.createTime})")
    /**
     * 依赖 trace_id 唯一索引执行 INSERT IGNORE；重复时返回零，业务层据此跳过统计累加。
     */
    int insertIgnore(@Param("log") CallLog log);

    @Select("<script>" +
            "SELECT COUNT(*) FROM tb_call_log WHERE create_time &gt;= #{start}" +
            "<if test='appId != null'> AND app_id = #{appId}</if>" +
            "</script>")
    /**
     * 统计起始时间以来的调用数，appId 为空时不限制应用。
     */
    long countSince(@Param("start") LocalDateTime start, @Param("appId") Long appId);

    @Select("<script>" +
            "SELECT COUNT(*) FROM tb_call_log WHERE create_time &gt;= #{start} " +
            "AND status_code &gt;= 200 AND status_code &lt; 300" +
            "<if test='appId != null'> AND app_id = #{appId}</if>" +
            "</script>")
    /**
     * 统计起始时间以来 HTTP 2xx 调用数，可按应用限制。
     */
    long countSuccessSince(@Param("start") LocalDateTime start, @Param("appId") Long appId);

    @Select("<script>" +
            "SELECT AVG(latency_ms) FROM tb_call_log WHERE create_time &gt;= #{start}" +
            "<if test='appId != null'> AND app_id = #{appId}</if>" +
            "</script>")
    /**
     * 计算指定时间与应用范围内平均耗时，无记录时可能返回空值。
     */
    Double averageLatencySince(@Param("start") LocalDateTime start, @Param("appId") Long appId);

    @Select("<script>" +
            "SELECT COUNT(DISTINCT app_id) FROM tb_call_log WHERE create_time &gt;= #{start}" +
            "<if test='appId != null'> AND app_id = #{appId}</if>" +
            "</script>")
    /**
     * 统计时间范围内出现的不同应用数，不等同所有启用应用数。
     */
    long countDistinctAppsSince(@Param("start") LocalDateTime start, @Param("appId") Long appId);

    @Select("<script>" +
            "SELECT api_id AS apiId, COUNT(*) AS callCount FROM tb_call_log " +
            "WHERE create_time &gt;= #{start} AND api_id IS NOT NULL" +
            "<if test='appId != null'> AND app_id = #{appId}</if>" +
            " GROUP BY api_id ORDER BY callCount DESC LIMIT 5" +
            "</script>")
    /**
     * 按调用数分组排序取前五 API，排除未识别 API 的日志。
     */
    List<Map<String, Object>> topApisSince(@Param("start") LocalDateTime start, @Param("appId") Long appId);
}
