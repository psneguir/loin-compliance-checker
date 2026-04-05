package com.loin.checker.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoinSearchRequest {
    private Integer lph;
    private Integer awf;
    private Integer page;
    private Integer size;

    public LoinSearchRequest() {}

    public Integer getLph() { return lph; }
    public void setLph(Integer lph) { this.lph = lph; }
    public Integer getAwf() { return awf; }
    public void setAwf(Integer awf) { this.awf = awf; }
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
}
