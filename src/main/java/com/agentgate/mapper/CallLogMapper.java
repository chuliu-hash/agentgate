package com.agentgate.mapper;

import com.agentgate.entity.CallLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface CallLogMapper extends BaseMapper<CallLog> {

    /**
     * 依赖 trace_id 唯一索引做幂等：重复消费返回 0，不抛 DuplicateKeyException。
     */
    @Insert("INSERT IGNORE INTO tb_call_log(trace_id, app_id, api_id, method, path, status_code, " +
            "latency_ms, client_ip, error_code, request_size, response_size, create_time) " +
            "VALUES(#{log.traceId}, #{log.appId}, #{log.apiId}, #{log.method}, #{log.path}, " +
            "#{log.statusCode}, #{log.latencyMs}, #{log.clientIp}, #{log.errorCode}, " +
            "#{log.requestSize}, #{log.responseSize}, #{log.createTime})")
    int insertIgnore(@Param("log") CallLog log);

    @Select("<script>" +
            "SELECT COUNT(*) FROM tb_call_log WHERE create_time &gt;= #{start}" +
            "<if test='appId != null'> AND app_id = #{appId}</if>" +
            "</script>")
    long countSince(@Param("start") LocalDateTime start, @Param("appId") Long appId);

    @Select("<script>" +
            "SELECT COUNT(*) FROM tb_call_log WHERE create_time &gt;= #{start} " +
            "AND status_code &gt;= 200 AND status_code &lt; 300" +
            "<if test='appId != null'> AND app_id = #{appId}</if>" +
            "</script>")
    long countSuccessSince(@Param("start") LocalDateTime start, @Param("appId") Long appId);

    @Select("<script>" +
            "SELECT AVG(latency_ms) FROM tb_call_log WHERE create_time &gt;= #{start}" +
            "<if test='appId != null'> AND app_id = #{appId}</if>" +
            "</script>")
    Double averageLatencySince(@Param("start") LocalDateTime start, @Param("appId") Long appId);

    @Select("<script>" +
            "SELECT COUNT(DISTINCT app_id) FROM tb_call_log WHERE create_time &gt;= #{start}" +
            "<if test='appId != null'> AND app_id = #{appId}</if>" +
            "</script>")
    long countDistinctAppsSince(@Param("start") LocalDateTime start, @Param("appId") Long appId);

    @Select("<script>" +
            "SELECT api_id AS apiId, COUNT(*) AS callCount FROM tb_call_log " +
            "WHERE create_time &gt;= #{start} AND api_id IS NOT NULL" +
            "<if test='appId != null'> AND app_id = #{appId}</if>" +
            " GROUP BY api_id ORDER BY callCount DESC LIMIT 5" +
            "</script>")
    List<Map<String, Object>> topApisSince(@Param("start") LocalDateTime start, @Param("appId") Long appId);
}
