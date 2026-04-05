package com.loin.checker.model;

public class LoinProperty {
    private String name;
    private String dataType;

    public LoinProperty() {}

    public LoinProperty(String name, String dataType) {
        this.name = name;
        this.dataType = dataType;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }

    @Override
    public String toString() {
        return "LoinProperty{name='" + name + "', dataType='" + dataType + "'}";
    }
}
