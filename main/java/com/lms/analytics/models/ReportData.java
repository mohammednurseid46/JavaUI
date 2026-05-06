package com.lms.analytics.models;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;

/**
 * DTO/Model for holding report generation parameters and results.
 */
public class ReportData {
    private String reportType;
    private String title;
    private LocalDateTime generatedAt;
    private Map<String, Object> parameters;
    private List<Map<String, Object>> rows;
    private Map<String, Object> summary;

    public ReportData() {
        this.generatedAt = LocalDateTime.now();
    }

    public ReportData(String reportType, String title) {
        this.reportType = reportType;
        this.title = title;
        this.generatedAt = LocalDateTime.now();
    }

    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    public Map<String, Object> getParameters() { return parameters; }
    public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }

    public List<Map<String, Object>> getRows() { return rows; }
    public void setRows(List<Map<String, Object>> rows) { this.rows = rows; }

    public Map<String, Object> getSummary() { return summary; }
    public void setSummary(Map<String, Object> summary) { this.summary = summary; }
}
