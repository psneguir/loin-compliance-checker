package com.loin.checker.model;

import java.util.ArrayList;
import java.util.List;

public class ComplianceResult {
    private IfcElement element;
    private LoinObjectType matchedObjectType;
    private boolean coveredByLoin;
    private List<PropertyResult> propertyResults;
    private double complianceRate;

    public ComplianceResult() {
        this.propertyResults = new ArrayList<>();
    }

    public IfcElement getElement() { return element; }
    public void setElement(IfcElement element) { this.element = element; }
    public LoinObjectType getMatchedObjectType() { return matchedObjectType; }
    public void setMatchedObjectType(LoinObjectType matchedObjectType) { this.matchedObjectType = matchedObjectType; }
    public boolean isCoveredByLoin() { return coveredByLoin; }
    public void setCoveredByLoin(boolean coveredByLoin) { this.coveredByLoin = coveredByLoin; }
    public List<PropertyResult> getPropertyResults() { return propertyResults; }
    public void setPropertyResults(List<PropertyResult> propertyResults) { this.propertyResults = propertyResults; }
    public double getComplianceRate() { return complianceRate; }
    public void setComplianceRate(double complianceRate) { this.complianceRate = complianceRate; }
}
