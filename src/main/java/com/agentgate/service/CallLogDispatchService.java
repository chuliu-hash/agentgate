package com.agentgate.service;

import com.agentgate.dto.CallLogMessage;
/**
 * 教学说明：业务服务契约，负责把调用日志优先投递到 MQ；发送异常时委托 CallLogService 同步落库兜底。
 * 文件路径：src/main/java/com/agentgate/service/CallLogDispatchService.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */

public interface CallLogDispatchService {

    /**
     * 尝试向 MQ 发送调用快照；发送抛异常时同步落库兜底，兜底失败仅记日志。
     * 发送未抛异常不等于最终持久化成功，本方法不向主流程抛异常。
     */
    void asyncSave(CallLogMessage message);
}
