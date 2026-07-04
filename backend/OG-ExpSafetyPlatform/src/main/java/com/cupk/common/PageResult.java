package com.cupk.common;

import lombok.Data;
import java.util.List;

/**
 * 分页查询结果封装
 * 成员A交付
 */
@Data
public class PageResult<T> {
    private List<T> records;
    private long total;
    private long pageNum;
    private long pageSize;

    public PageResult() {}

    public PageResult(List<T> records, long total, long pageNum, long pageSize) {
        this.records = records;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }
}
