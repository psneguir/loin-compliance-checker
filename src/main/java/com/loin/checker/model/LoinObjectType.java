package com.loin.checker.model;

import java.util.ArrayList;
import java.util.List;

public class LoinObjectType {
    private String name;
    private String ifcType;
    private List<LoinPropertySet> propertySets;

    public LoinObjectType() {
        this.propertySets = new ArrayList<>();
    }

    public LoinObjectType(String name, String ifcType, List<LoinPropertySet> propertySets) {
        this.name = name;
        this.ifcType = ifcType;
        this.propertySets = propertySets != null ? propertySets : new ArrayList<>();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIfcType() { return ifcType; }
    public void setIfcType(String ifcType) { this.ifcType = ifcType; }
    public List<LoinPropertySet> getPropertySets() { return propertySets; }
    public void setPropertySets(List<LoinPropertySet> propertySets) { this.propertySets = propertySets; }
}
