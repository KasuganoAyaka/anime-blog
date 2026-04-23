package com.animeblog.dto;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AdminPageResult<T> {

    private List<T> records;
    private long total;
    private long current;
    private long size;
    private Map<String, Long> summary;

    public static <T> AdminPageResult<T> of(
            List<T> records,
            long current,
            long size,
            long total
    ) {
        return of(records, current, size, total, Collections.emptyMap());
    }

    public static <T> AdminPageResult<T> of(
            List<T> records,
            long current,
            long size,
            long total,
            Map<String, Long> summary
    ) {
        AdminPageResult<T> result = new AdminPageResult<>();
        result.setRecords(records == null ? Collections.emptyList() : records);
        result.setCurrent(Math.max(1L, current));
        result.setSize(Math.max(1L, size));
        result.setTotal(Math.max(0L, total));
        result.setSummary(summary == null ? Collections.emptyMap() : summary);
        return result;
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getCurrent() {
        return current;
    }

    public void setCurrent(long current) {
        this.current = current;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public Map<String, Long> getSummary() {
        return summary;
    }

    public void setSummary(Map<String, Long> summary) {
        this.summary = summary;
    }
}
