package com.loin.checker.model;

public class PropertyResult {
    public enum Status { PASS, MISSING, INCOMPLETE }

    private String propertySetName;
    private String propertyName;
    private String value;
    private Status status;

    public PropertyResult() {}

    public PropertyResult(String propertySetName, String propertyName, String value, Status status) {
        this.propertySetName = propertySetName;
        this.propertyName = propertyName;
        this.value = value;
        this.status = status;
    }

    public String getPropertySetName() { return propertySetName; }
    public void setPropertySetName(String propertySetName) { this.propertySetName = propertySetName; }
    public String getPropertyName() { return propertyName; }
    public void setPropertyName(String propertyName) { this.propertyName = propertyName; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}
