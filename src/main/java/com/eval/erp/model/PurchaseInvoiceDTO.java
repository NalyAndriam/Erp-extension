package com.eval.erp.model;

import java.util.List;
import java.util.Map;

public class PurchaseInvoiceDTO {
    private String name;
    private String supplier;
    private String status;
    private Double grandTotal;
    private Double outstandingAmount;
    private String postingDate;
    private String dueDate;
    private List<Map<String, Object>> items; // Optional, used for invoice details
    private Map<String, Object> additionalFields; // For flexibility with ERPNext fields

    // Constructor
    public PurchaseInvoiceDTO() {}

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Double getGrandTotal() { return grandTotal; }
    public void setGrandTotal(Double grandTotal) { this.grandTotal = grandTotal; }
    public Double getOutstandingAmount() { return outstandingAmount; }
    public void setOutstandingAmount(Double outstandingAmount) { this.outstandingAmount = outstandingAmount; }
    public String getPostingDate() { return postingDate; }
    public void setPostingDate(String postingDate) { this.postingDate = postingDate; }
    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }
    public List<Map<String, Object>> getItems() { return items; }
    public void setItems(List<Map<String, Object>> items) { this.items = items; }
    public Map<String, Object> getAdditionalFields() { return additionalFields; }
    public void setAdditionalFields(Map<String, Object> additionalFields) { this.additionalFields = additionalFields; }
}