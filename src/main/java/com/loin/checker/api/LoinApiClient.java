package com.loin.checker.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loin.checker.model.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoinApiClient {

    private static final String BASE_URL = "https://via.bund.de/bim/aia/api/v1/public/loin";
    private static final Pattern LPH_PATTERN = Pattern.compile("LPH\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern AWF_PATTERN = Pattern.compile("AwF\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public LoinApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public LoinSpec getLoinByGuid(String guid) {
        String url = BASE_URL + "/" + guid;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) {
                throw new RuntimeException("LOIN not found: " + guid);
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("API error " + response.statusCode() + ": " + response.body());
            }
            return parseLoinSpec(response.body());
        } catch (IOException e) {
            throw new RuntimeException("Network error fetching LOIN " + guid + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Request interrupted for LOIN " + guid, e);
        }
    }

    public List<LoinSpec> searchLoins(Integer lph, Integer awf, int page, int size) {
        LoinSearchRequest requestBody = new LoinSearchRequest();
        requestBody.setLph(lph);
        requestBody.setAwf(awf);
        requestBody.setPage(page);
        requestBody.setSize(size);

        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(requestBody);
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize search request: " + e.getMessage(), e);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("API error " + response.statusCode() + ": " + response.body());
            }
            return parseSearchResults(response.body());
        } catch (IOException e) {
            throw new RuntimeException("Network error during search: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Search request interrupted", e);
        }
    }

    private LoinSpec parseLoinSpec(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            return buildLoinSpec(root);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse LOIN JSON response: " + e.getMessage(), e);
        }
    }

    private LoinSpec buildLoinSpec(JsonNode root) {
        LoinSpec spec = new LoinSpec();
        spec.setGuid(getText(root, "guid"));
        spec.setName(getText(root, "name"));
        spec.setDescription(getText(root, "description"));

        // Try direct lph/awf fields first, then parse from name
        if (root.has("lph") && !root.get("lph").isNull()) {
            spec.setLph(root.get("lph").asText());
        }
        if (root.has("awf") && !root.get("awf").isNull()) {
            spec.setAwf(root.get("awf").asText());
        }
        // Parse LPH/AwF from name if not set
        if (spec.getName() != null) {
            if (spec.getLph() == null) {
                Matcher m = LPH_PATTERN.matcher(spec.getName());
                if (m.find()) spec.setLph(m.group(1));
            }
            if (spec.getAwf() == null) {
                Matcher m = AWF_PATTERN.matcher(spec.getName());
                if (m.find()) spec.setAwf(m.group(1));
            }
        }

        List<LoinObjectType> objectTypes = new ArrayList<>();
        JsonNode otNode = root.get("objectTypes");
        if (otNode != null && otNode.isArray()) {
            for (JsonNode ot : otNode) {
                objectTypes.add(buildObjectType(ot));
            }
        }
        spec.setObjectTypes(objectTypes);
        return spec;
    }

    private LoinObjectType buildObjectType(JsonNode node) {
        LoinObjectType objectType = new LoinObjectType();
        objectType.setName(getText(node, "name"));
        objectType.setIfcType(getText(node, "ifcType") != null ? getText(node, "ifcType") : objectType.getName());

        List<LoinPropertySet> psets = new ArrayList<>();
        JsonNode psetsNode = node.get("propertySets");
        if (psetsNode != null && psetsNode.isArray()) {
            for (JsonNode pset : psetsNode) {
                psets.add(buildPropertySet(pset));
            }
        }
        objectType.setPropertySets(psets);
        return objectType;
    }

    private LoinPropertySet buildPropertySet(JsonNode node) {
        LoinPropertySet pset = new LoinPropertySet();
        pset.setName(getText(node, "name"));

        List<LoinProperty> props = new ArrayList<>();
        JsonNode propsNode = node.get("properties");
        if (propsNode != null && propsNode.isArray()) {
            for (JsonNode p : propsNode) {
                LoinProperty prop = new LoinProperty();
                prop.setName(getText(p, "name"));
                prop.setDataType(getText(p, "dataType"));
                props.add(prop);
            }
        }
        pset.setProperties(props);
        return pset;
    }

    private List<LoinSpec> parseSearchResults(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            List<LoinSpec> results = new ArrayList<>();
            // API may return array or object with content field
            JsonNode items = root;
            if (root.isObject()) {
                if (root.has("content")) items = root.get("content");
                else if (root.has("data")) items = root.get("data");
                else if (root.has("items")) items = root.get("items");
            }
            if (items.isArray()) {
                for (JsonNode item : items) {
                    results.add(buildLoinSpec(item));
                }
            }
            return results;
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse search results: " + e.getMessage(), e);
        }
    }

    private String getText(JsonNode node, String field) {
        if (node == null || !node.has(field)) return null;
        JsonNode v = node.get(field);
        if (v.isNull()) return null;
        return v.asText();
    }
}
