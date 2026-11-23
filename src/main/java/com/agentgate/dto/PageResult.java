package com.agentgate.dto;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
/**
 * 教学说明：将 MyBatis IPage 或记录列表转换为 records/total/current/size，统一分页 JSON 形状。
 * 文件路径：src/main/java/com/agentgate/dto/PageResult.java
 * 阅读建议：先看字段和入口方法，再沿调用链阅读异常分支与数据访问。
 */
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<T> records;
    private long total;
    private long current;
    private long size;

    /**
     * 将分页结果转换为统一 records/total/current/size 结构。
     */
    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(page.getRecords());
        result.setTotal(page.getTotal());
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        return result;
    }

    /**
     * 将分页结果转换为统一 records/total/current/size 结构。
     */
    public static <T> PageResult<T> of(List<T> records, long total, long current, long size) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        result.setCurrent(current);
        result.setSize(size);
        return result;
    }
}
