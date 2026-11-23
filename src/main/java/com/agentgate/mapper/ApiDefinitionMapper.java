package com.agentgate.mapper;

import com.agentgate.entity.ApiDefinition;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
/**
 * 教学说明：继承 BaseMapper<ApiDefinition> 的数据库访问接口。selectById/selectList/insert/update 等通用 SQL 由 MyBatis-Plus 提供；空接口表示无自定义 SQL，不表示不执行数据库操作。
 * 文件路径：src/main/java/com/agentgate/mapper/ApiDefinitionMapper.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */

public interface ApiDefinitionMapper extends BaseMapper<ApiDefinition> {
}
