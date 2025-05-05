package com.eval.erp.model;

public class SupplierDTO {
    private String name;
    private String supplierName;
    private String supplierGroup;
    private String disabled;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public String getSupplierGroup() { return supplierGroup; }
    public void setSupplierGroup(String supplierGroup) { this.supplierGroup = supplierGroup; }
    public String getDisabled() { return disabled; }
    public void setDisabled(String disabled) { this.disabled = disabled; }
}