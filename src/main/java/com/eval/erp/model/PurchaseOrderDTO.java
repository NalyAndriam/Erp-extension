package com.eval.erp.model;

public class PurchaseOrderDTO {
    private String name;
    private String supplier;
    private String supplierName;
    private String status;
    private String transactionDate;
    private Double grandTotal;
    private Double perBilled;
    private Double perReceived;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTransactionDate() { return transactionDate; }
    public void setTransactionDate(String transactionDate) { this.transactionDate = transactionDate; }
    public Double getGrandTotal() { return grandTotal; }
    public void setGrandTotal(Double grandTotal) { this.grandTotal = grandTotal; }
    public Double getPerBilled() { return perBilled; }
    public void setPerBilled(Double perBilled) { this.perBilled = perBilled; }
    public Double getPerReceived() { return perReceived; }
    public void setPerReceived(Double perReceived) { this.perReceived = perReceived; }
}