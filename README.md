# LOIN Compliance Checker for BIM Models

A Java command-line application that checks IFC/BIM model files for compliance against LOIN (Level of Information Need) specifications from the German BIM portal (via.bund.de).

---

## Architecture and Design Rationale

### Component Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         Main (Entry Point)                       │
│                      app/Main.java                               │
└──────────┬──────────────────┬───────────────────────────────────┘
           │                  │
    ┌──────▼──────┐    ┌──────▼──────┐
    │ CliParser   │    │LoinApiClient│ ──► https://via.bund.de/...
    │ (CLI args)  │    │ (REST API)  │
    └─────────────┘    └──────┬──────┘
                              │ LoinSpec
           ┌──────────────────┼──────────────────┐
           │                  │                  │
    ┌──────▼──────┐    ┌──────▼──────┐    ┌──────▼──────┐
    │  IfcParser  │    │ Compliance  │    │ HtmlReport  │
    │ (.ifc file) │    │  Validator  │    │  Generator  │
    └──────┬──────┘    └──────┬──────┘    └──────┬──────┘
           │                  │                  │
    ┌──────▼──────┐    ┌──────▼──────┐    ┌──────▼──────┐
    │ IfcElement  │    │Compliance   │    │  HTML file  │
    │   (model)   │    │  Result     │    │  (report)   │
    └─────────────┘    └─────────────┘    └─────────────┘
```

### Data Flow

1. **CLI → Main**: Arguments parsed (IFC path, LOIN GUID, report path)
2. **Main → LoinApiClient**: Fetch LOIN specification via REST API
3. **Main → IfcParser**: Parse IFC STEP file into IfcElement objects
4. **Main → ComplianceValidator**: Cross-reference elements against LOIN spec
5. **Main → HtmlReportGenerator**: Produce color-coded HTML compliance report

### Design Decisions

| Decision | Rationale |
|---|---|
| **Java built-in HttpClient** | Zero extra dependencies; available since Java 11 |
| **Jackson JsonNode traversal** | API response structure may vary; avoids brittle POJO mapping |
| **Text-based IFC STEP parser** | No suitable free Java IFC library; STEP format is well-specified |
| **Fat JAR packaging** | Single artifact simplifies deployment and usage |
| **HTML report output** | Rich visualization; no runtime dependency on report viewer |

### Known Limitations

- Only parses a subset of IFC entity types (walls, roofs, slabs, doors, windows, beams, columns, stairs, furnishing)
- IFC geometry and spatial structure are not parsed (only property sets)
- The LOIN API at `via.bund.de` requires internet access
- Multi-line IFC STEP entities are not fully supported (rare in practice)
- No support for IFC relationships beyond `IFCRELDEFINESBYPROPERTIES`

---

## Setup & User Manual

### Prerequisites

| Tool | Version |
|---|---|
| Java JDK | 17 or higher |
| Apache Maven | 3.6 or higher |
| IDE (optional) | Eclipse 2023+ or IntelliJ IDEA |
| Internet access | Required for LOIN API |

### Dependencies

| Library | Version | Purpose |
|---|---|---|
| jackson-databind | 2.15.2 | JSON parsing of API responses |
| commons-cli | 1.5.0 | Command-line argument parsing |

### Build

```bash
git clone <repository-url>
cd loin-compliance-checker
mvn clean package
```

This produces `target/loin-checker.jar` (fat JAR with all dependencies).

### Run

```bash
java -jar target/loin-checker.jar --help
```

### CLI Argument Reference

| Argument | Required | Description |
|---|---|---|
| `--ifc <path>` | Yes (check mode) | Path to IFC model file |
| `--loin-guid <guid>` | Yes (check mode) | GUID of the LOIN spec |
| `--report <path>` | No | Report output path (default: `reports/`) |
| `--search` | No | Enable search/listing mode |
| `--lph <n>` | No | Filter by LPH in search mode |
| `--awf <n>` | No | Filter by AwF in search mode |
| `--help` / `-h` | No | Print help message |

### Example Terminal Output

```
Loading LOIN specification...
  Name:  Dach: LPH 1 - AwF 100
  GUID:  608e3ec2-ce6a-4f0f-8d96-2830e88a1e2e
  LPH:   1
  AwF:   100
  Object Types: 1

Parsing IFC model: AC20-FZK-Haus.ifc
  Found 42 relevant IFC elements.

Running compliance validation...
  Elements covered by LOIN: 3 / 42
  Elements not covered:     39
  Overall compliance rate:  66.7%
  Properties PASS:          4
  Properties MISSING:       2
  Properties INCOMPLETE:    0

Generating HTML report...
Report generated: reports/loin-report-20231201-143022.html
```

---

## Testing & Example Workflow

### FZK-Haus Walkthrough

The FZK-Haus is a publicly available IFC test model from KIT (Karlsruhe Institute of Technology).

**Step 1: Download the FZK-Haus IFC file**

```bash
curl -L -o AC20-FZK-Haus.ifc "http://www.ifcwiki.org/images/e/e3/AC20-FZK-Haus.ifc"
```

**Step 2: Find a LOIN specification**

```bash
java -jar target/loin-checker.jar --search
```

Or filter by LPH and AwF:

```bash
java -jar target/loin-checker.jar --search --lph 1 --awf 100
```

**Step 3: Run the compliance check**

```bash
java -jar target/loin-checker.jar \
  --ifc AC20-FZK-Haus.ifc \
  --loin-guid 608e3ec2-ce6a-4f0f-8d96-2830e88a1e2e \
  --report reports/
```

**Step 4: Open the report**

```bash
# Linux
xdg-open reports/loin-report-*.html

# macOS
open reports/loin-report-*.html

# Windows
start reports/loin-report-*.html
```

### Full Example Commands

```bash
# Build
mvn clean package

# Search all LOIN specs (first 20)
java -jar target/loin-checker.jar --search

# Search with filters
java -jar target/loin-checker.jar --search --lph 2 --awf 200

# Run compliance check with default report path
java -jar target/loin-checker.jar --ifc model.ifc --loin-guid <guid>

# Run compliance check with custom report path
java -jar target/loin-checker.jar --ifc model.ifc --loin-guid <guid> --report output/my-report.html
```

### Edge Case Behavior

| Scenario | Behavior |
|---|---|
| IFC element has no matching LOIN object type | Marked as "NOT COVERED", excluded from compliance rate |
| Property exists but value is empty or "$" | Status: INCOMPLETE (orange) |
| Property is entirely absent | Status: MISSING (red) |
| Property has a non-empty value | Status: PASS (green) |
| LOIN API returns 404 | Error: "LOIN not found: <guid>" |
| IFC file not found | Error: "IFC file not found: <path>" |
| No elements found in IFC | Report generated with 0 elements |
| LOIN has no object types | All elements marked not covered |
