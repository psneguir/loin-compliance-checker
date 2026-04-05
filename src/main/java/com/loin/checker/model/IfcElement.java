package com.loin.checker.model;

import java.util.HashMap;
import java.util.Map;

public class IfcElement {
    private String id;
    private String globalId;
    private String ifcClass;
    private String name;
    private Map<String, Map<String, String>> propertySets;

    public IfcElement() {
        this.propertySets = new HashMap<>();
    }

    public IfcElement(String id, String globalId, String ifcClass, String name) {
        this.id = id;
        this.globalId = globalId;
        this.ifcClass = ifcClass;
        this.name = name;
        this.propertySets = new HashMap<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getGlobalId() { return globalId; }
    public void setGlobalId(String globalId) { this.globalId = globalId; }
    public String getIfcClass() { return ifcClass; }
    public void setIfcClass(String ifcClass) { this.ifcClass = ifcClass; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Map<String, Map<String, String>> getPropertySets() { return propertySets; }
    public void setPropertySets(Map<String, Map<String, String>> propertySets) { this.propertySets = propertySets; }
}
