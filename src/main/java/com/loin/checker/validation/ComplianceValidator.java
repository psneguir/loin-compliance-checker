package com.loin.checker.validation;

import com.loin.checker.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ComplianceValidator {

    public List<ComplianceResult> validate(LoinSpec loin, List<IfcElement> elements) {
        List<ComplianceResult> results = new ArrayList<>();
        for (IfcElement element : elements) {
            results.add(validateElement(loin, element));
        }
        return results;
    }

    private ComplianceResult validateElement(LoinSpec loin, IfcElement element) {
        ComplianceResult result = new ComplianceResult();
        result.setElement(element);

        LoinObjectType matchedType = findMatchingObjectType(loin, element);
        if (matchedType == null) {
            result.setCoveredByLoin(false);
            result.setComplianceRate(0.0);
            return result;
        }

        result.setCoveredByLoin(true);
        result.setMatchedObjectType(matchedType);

        List<PropertyResult> propertyResults = new ArrayList<>();
        int total = 0;
        int passed = 0;

        for (LoinPropertySet requiredPset : matchedType.getPropertySets()) {
            String psetName = requiredPset.getName();
            Map<String, String> actualProps = element.getPropertySets().get(psetName);

            for (LoinProperty requiredProp : requiredPset.getProperties()) {
                total++;
                String propName = requiredProp.getName();
                PropertyResult pr = new PropertyResult();
                pr.setPropertySetName(psetName);
                pr.setPropertyName(propName);

                if (actualProps == null || !actualProps.containsKey(propName)) {
                    pr.setStatus(PropertyResult.Status.MISSING);
                } else {
                    String value = actualProps.get(propName);
                    pr.setValue(value);
                    if (value == null || value.isEmpty() || value.equals("$") || value.equalsIgnoreCase("NULL")) {
                        pr.setStatus(PropertyResult.Status.INCOMPLETE);
                    } else {
                        pr.setStatus(PropertyResult.Status.PASS);
                        passed++;
                    }
                }
                propertyResults.add(pr);
            }
        }

        result.setPropertyResults(propertyResults);
        result.setComplianceRate(total > 0 ? (100.0 * passed / total) : 100.0);
        return result;
    }

    private LoinObjectType findMatchingObjectType(LoinSpec loin, IfcElement element) {
        String ifcClass = element.getIfcClass().toUpperCase();
        for (LoinObjectType ot : loin.getObjectTypes()) {
            String otName = ot.getName().toUpperCase();
            // Direct match
            if (ifcClass.equals(otName)) return ot;
            // Without IFC prefix comparison
            String otStripped = otName.startsWith("IFC") ? otName.substring(3) : otName;
            String elStripped = ifcClass.startsWith("IFC") ? ifcClass.substring(3) : ifcClass;
            if (otStripped.equals(elStripped)) return ot;
            // Handle WALLSTANDARDCASE -> WALL
            if (elStripped.startsWith(otStripped)) return ot;
            // Handle TYPE suffix: IFCROOFTYPE -> IFCROOF
            if (ifcClass.equals(otName + "TYPE")) return ot;
            if ((ifcClass + "TYPE").equals(otName)) return ot;
        }
        return null;
    }

    public long countCovered(List<ComplianceResult> results) {
        return results.stream().filter(ComplianceResult::isCoveredByLoin).count();
    }

    public double overallComplianceRate(List<ComplianceResult> results) {
        List<ComplianceResult> covered = results.stream()
                .filter(ComplianceResult::isCoveredByLoin).toList();
        if (covered.isEmpty()) return 0.0;
        return covered.stream().mapToDouble(ComplianceResult::getComplianceRate).average().orElse(0.0);
    }

    public long countByStatus(List<ComplianceResult> results, PropertyResult.Status status) {
        return results.stream()
                .flatMap(r -> r.getPropertyResults().stream())
                .filter(p -> p.getStatus() == status)
                .count();
    }
}
