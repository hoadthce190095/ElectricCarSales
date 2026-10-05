package vn.edu.fpt.electriccarsales.dto;

import java.util.List;

public class PageResponse<T> {
    private List<T> items;
    private int page;
    private long total;
    private int pageSize;

    public PageResponse() {
    }

    public PageResponse(List<T> items, int page, long total, int pageSize) {
        this.items = items;
        this.page = page;
        this.total = total;
        this.pageSize = pageSize;
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
