package com.loin.checker.ifc;

import com.loin.checker.model.IfcElement;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IfcParser {

    // Common architectural/structural IFC element types that carry property sets
    // relevant to LOIN compliance checks. Type entities (e.g. IFCWALLTYPE) are
    // excluded as they describe templates rather than placed instances.
    private static final Set<String> TARGET_CLASSES = Set.of(
            "IFCWALL", "IFCWALLSTANDARDCASE", "IFCROOF", "IFCSLAB",
            "IFCDOOR", "IFCWINDOW", "IFCBEAM", "IFCCOLUMN",
            "IFCSTAIR", "IFCFURNISHINGELEMENT"
    );

    private static final Pattern ENTITY_PATTERN = Pattern.compile(
            "^#(\\d+)\\s*=\\s*([A-Z][A-Z0-9]*)\\s*\\((.*)\\)\\s*;\\s*$"
    );

    // Map: id -> [entityName, rawArgs]
    private final Map<String, String[]> entityMap = new HashMap<>();

    public List<IfcElement> parse(String filePath) {
        entityMap.clear();
        try {
            readEntities(filePath);
            return buildElements();
        } catch (FileNotFoundException e) {
            throw new RuntimeException("IFC file not found: " + filePath, e);
        } catch (IOException e) {
            throw new RuntimeException("Error reading IFC file: " + e.getMessage(), e);
        }
    }

    private void readEntities(String filePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            StringBuilder currentLine = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("/*") || line.startsWith("//")) continue;
                currentLine.append(line);
                // IFC STEP lines end with semicolon
                if (line.endsWith(";")) {
                    String fullLine = currentLine.toString().trim();
                    parseEntityLine(fullLine);
                    currentLine = new StringBuilder();
                }
                // handle multi-line (rare but possible)
            }
        }
    }

    private void parseEntityLine(String line) {
        Matcher m = ENTITY_PATTERN.matcher(line);
        if (!m.matches()) return;
        String id = "#" + m.group(1);
        String entityName = m.group(2).toUpperCase();
        String rawArgs = m.group(3);
        entityMap.put(id, new String[]{entityName, rawArgs});
    }

    private List<IfcElement> buildElements() {
        // Build elements from target IFC classes
        Map<String, IfcElement> elements = new LinkedHashMap<>();
        for (Map.Entry<String, String[]> entry : entityMap.entrySet()) {
            String id = entry.getKey();
            String[] data = entry.getValue();
            String entityName = data[0];
            if (TARGET_CLASSES.contains(entityName)) {
                List<String> attrs = splitAttributes(data[1]);
                String globalId = extractString(attrs, 0);
                String name = extractString(attrs, 2);
                IfcElement el = new IfcElement(id, globalId, entityName, name);
                elements.put(id, el);
            }
        }

        // Process IFCPROPERTYSINGLEVALUE
        Map<String, String[]> propValues = new HashMap<>(); // id -> [name, value]
        for (Map.Entry<String, String[]> entry : entityMap.entrySet()) {
            if ("IFCPROPERTYSINGLEVALUE".equals(entry.getValue()[0])) {
                List<String> attrs = splitAttributes(entry.getValue()[1]);
                String propName = extractString(attrs, 0);
                String value = parsePropertyValue(attrs.size() > 2 ? attrs.get(2) : "$");
                propValues.put(entry.getKey(), new String[]{propName, value});
            }
        }

        // Process IFCPROPERTYSET: id -> [psetName, propIds...]
        Map<String, String[]> psets = new HashMap<>();
        for (Map.Entry<String, String[]> entry : entityMap.entrySet()) {
            if ("IFCPROPERTYSET".equals(entry.getValue()[0])) {
                List<String> attrs = splitAttributes(entry.getValue()[1]);
                String psetName = extractString(attrs, 2);
                String propListStr = attrs.size() > 4 ? attrs.get(4) : "";
                List<String> propIds = extractRefs(propListStr);
                String[] psetData = new String[1 + propIds.size()];
                psetData[0] = psetName;
                for (int i = 0; i < propIds.size(); i++) psetData[i + 1] = propIds.get(i);
                psets.put(entry.getKey(), psetData);
            }
        }

        // Process IFCRELDEFINESBYPROPERTIES to link psets to elements
        for (Map.Entry<String, String[]> entry : entityMap.entrySet()) {
            if ("IFCRELDEFINESBYPROPERTIES".equals(entry.getValue()[0])) {
                List<String> attrs = splitAttributes(entry.getValue()[1]);
                // attr[4] = related objects list, attr[5] = relating property definition
                String elemListStr = attrs.size() > 4 ? attrs.get(4) : "";
                String psetRef = attrs.size() > 5 ? attrs.get(5).trim() : "";

                List<String> elemIds = extractRefs(elemListStr);
                if (!psets.containsKey(psetRef)) continue;

                String[] psetData = psets.get(psetRef);
                String psetName = psetData[0];
                if (psetName == null || psetName.isEmpty()) continue;

                // Build the property map for this pset
                Map<String, String> propMap = new LinkedHashMap<>();
                for (int i = 1; i < psetData.length; i++) {
                    String propId = psetData[i];
                    String[] pv = propValues.get(propId);
                    if (pv != null && pv[0] != null && !pv[0].isEmpty()) {
                        propMap.put(pv[0], pv[1]);
                    }
                }

                // Link to elements
                for (String elemId : elemIds) {
                    IfcElement el = elements.get(elemId);
                    if (el != null) {
                        el.getPropertySets().put(psetName, propMap);
                    }
                }
            }
        }

        return new ArrayList<>(elements.values());
    }

    /**
     * Splits an IFC STEP attribute string on top-level commas, correctly handling:
     * <ul>
     *   <li>Nested parentheses: e.g. {@code (#1,#2),(#3)} is kept intact as one token</li>
     *   <li>Single-quoted strings: e.g. {@code 'O''Brien'} with escaped internal quotes ('')</li>
     *   <li>Typed values: e.g. {@code IFCLABEL('value')}</li>
     * </ul>
     *
     * @param raw the raw comma-separated attribute string from an IFC STEP entity line
     * @return ordered list of individual attribute tokens
     */
    List<String> splitAttributes(String raw) {
        List<String> attrs = new ArrayList<>();
        int depth = 0;
        boolean inString = false;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '\'' && !inString) {
                inString = true;
                current.append(c);
            } else if (c == '\'' && inString) {
                // Check for escaped quote ''
                if (i + 1 < raw.length() && raw.charAt(i + 1) == '\'') {
                    current.append(c);
                    i++; // skip next quote
                } else {
                    inString = false;
                    current.append(c);
                }
            } else if (!inString && c == '(') {
                depth++;
                current.append(c);
            } else if (!inString && c == ')') {
                depth--;
                current.append(c);
            } else if (!inString && c == ',' && depth == 0) {
                attrs.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0 || raw.endsWith(",")) {
            attrs.add(current.toString().trim());
        }
        return attrs;
    }

    private String extractString(List<String> attrs, int index) {
        if (index >= attrs.size()) return null;
        String val = attrs.get(index).trim();
        if (val.equals("$") || val.equals("*")) return null;
        if (val.startsWith("'") && val.endsWith("'") && val.length() >= 2) {
            return val.substring(1, val.length() - 1).replace("''", "'");
        }
        return val;
    }

    private List<String> extractRefs(String raw) {
        List<String> refs = new ArrayList<>();
        raw = raw.trim();
        if (raw.startsWith("(") && raw.endsWith(")")) {
            raw = raw.substring(1, raw.length() - 1);
        }
        for (String part : raw.split(",")) {
            part = part.trim();
            if (part.startsWith("#")) {
                refs.add(part);
            }
        }
        return refs;
    }

    private String parsePropertyValue(String raw) {
        if (raw == null || raw.equals("$") || raw.equals("*")) return null;
        raw = raw.trim();
        // Handle typed values like IFCLABEL('value'), IFCTEXT('value'), etc.
        int parenIdx = raw.indexOf('(');
        if (parenIdx > 0 && raw.endsWith(")")) {
            String inner = raw.substring(parenIdx + 1, raw.length() - 1).trim();
            // Handle boolean .T. .F.
            if (inner.equals(".T.")) return "true";
            if (inner.equals(".F.")) return "false";
            // Unwrap string
            if (inner.startsWith("'") && inner.endsWith("'") && inner.length() >= 2) {
                return inner.substring(1, inner.length() - 1).replace("''", "'");
            }
            return inner;
        }
        // Plain string
        if (raw.startsWith("'") && raw.endsWith("'") && raw.length() >= 2) {
            return raw.substring(1, raw.length() - 1).replace("''", "'");
        }
        // Boolean
        if (raw.equals(".T.")) return "true";
        if (raw.equals(".F.")) return "false";
        return raw;
    }
}
