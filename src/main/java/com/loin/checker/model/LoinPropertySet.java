package com.loin.checker.model;

import java.util.ArrayList;
import java.util.List;

public class LoinPropertySet {
    private String name;
    private List<LoinProperty> properties;

    public LoinPropertySet() {
        this.properties = new ArrayList<>();
    }

    public LoinPropertySet(String name, List<LoinProperty> properties) {
        this.name = name;
        this.properties = properties != null ? properties : new ArrayList<>();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<LoinProperty> getProperties() { return properties; }
    public void setProperties(List<LoinProperty> properties) { this.properties = properties; }
}
