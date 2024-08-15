package com.dht.store.utils;

import cn.hutool.core.util.PageUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

public class BasePage<T> implements Page<T> {
    private List<T> list;
    private int pageNum;
    private int pageSize;
    private long total;
    private Pageable pageable;

    public BasePage(List<T> list, int pageNum, int pageSize, long total) {
        this.list = list;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.total = total;
        this.pageable = PageRequest.ofSize(pageSize);
    }


    @Override
    public Pageable getPageable() {
        return pageable;
    }

    public int getPageNum() {
        return pageNum;
    }

    public int getPageSize() {
        return pageSize;
    }

    public long getTotal() {
        return total;
    }

    @Override
    public int getTotalPages() {
        return PageUtil.totalPage(total,pageSize);
    }

    @Override
    public long getTotalElements() {
        return total;
    }

    @Override
    public Page map(Function converter) {
        return null;
    }

    @Override
    public int getNumber() {
        return 0;
    }

    @Override
    public int getSize() {
        return list.size();
    }

    @Override
    public int getNumberOfElements() {
        return 0;
    }

    @Override
    public List<T> getContent() {
        return list;
    }

    @Override
    public boolean hasContent() {
        return false;
    }

    @Override
    public Sort getSort() {
        return null;
    }

    @Override
    public boolean isFirst() {
        return pageNum == 0;
    }

    @Override
    public boolean isLast() {
        return pageNum + 1 >= getTotalPages();
    }

    @Override
    public boolean hasNext() {
        return pageNum + 1 < getTotalPages();
    }

    @Override
    public boolean hasPrevious() {
        return false;
    }

    @Override
    public Pageable nextPageable() {
        return null;
    }

    @Override
    public Pageable previousPageable() {
        return null;
    }

    @Override
    public Iterator<T> iterator() {
        return list.iterator();
    }
}
