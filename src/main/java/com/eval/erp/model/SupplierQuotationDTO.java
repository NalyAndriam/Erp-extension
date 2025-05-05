package com.eval.erp.model;

import java.util.List;
import java.util.Map;

public class SupplierQuotationDTO {
    private String name;
    private String title;
    private String status;
    private String supplier;
    private String transactionDate;
    private Double grandTotal;
    private List<Map<String, Object>> items;
    private Integer docstatus;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }
    public String getTransactionDate() { return transactionDate; }
    public void setTransactionDate(String transactionDate) { this.transactionDate = transactionDate; }
    public Double getGrandTotal() { return grandTotal; }
    public void setGrandTotal(Double grandTotal) { this.grandTotal = grandTotal; }
    public List<Map<String, Object>> getItems() { return items; }
    public void setItems(List<Map<String, Object>> items) { this.items = items; }
    public Integer getDocstatus() { return docstatus; }
    public void setDocstatus(Integer docstatus) { this.docstatus = docstatus; }
}