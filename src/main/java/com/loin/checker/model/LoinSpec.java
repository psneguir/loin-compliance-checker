package com.loin.checker.model;

import java.util.ArrayList;
import java.util.List;

public class LoinSpec {
    private String guid;
    private String name;
    private String lph;
    private String awf;
    private String description;
    private List<LoinObjectType> objectTypes;

    public LoinSpec() {
        this.objectTypes = new ArrayList<>();
    }

    public String getGuid() { return guid; }
    public void setGuid(String guid) { this.guid = guid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLph() { return lph; }
    public void setLph(String lph) { this.lph = lph; }
    public String getAwf() { return awf; }
    public void setAwf(String awf) { this.awf = awf; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<LoinObjectType> getObjectTypes() { return objectTypes; }
    public void setObjectTypes(List<LoinObjectType> objectTypes) { this.objectTypes = objectTypes; }
}
