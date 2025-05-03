package com.eval.erp.model;

import java.util.List;

public class QuotationUpdateDTO {
    private String name;
    private List<ItemUpdateDTO> items;

    // Getters et setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ItemUpdateDTO> getItems() {
        return items;
    }

    public void setItems(List<ItemUpdateDTO> items) {
        this.items = items;
    }

    public static class ItemUpdateDTO {
        private String name;
        private String itemCode;
        private String itemName;
        private String qty;
        private String rate;
        private String stockUom;
        private String uom;
        private String conversionFactor;
        private String baseRate;
        private String baseAmount;
        private String warehouse;

        // Getters et setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getItemCode() {
            return itemCode;
        }

        public void setItemCode(String itemCode) {
            this.itemCode = itemCode;
        }

        public String getItemName() {
            return itemName;
        }

        public void setItemName(String itemName) {
            this.itemName = itemName;
        }

        public String getQty() {
            return qty;
        }

        public void setQty(String qty) {
            this.qty = qty;
        }

        public String getRate() {
            return rate;
        }

        public void setRate(String rate) {
            this.rate = rate;
        }

        public String getStockUom() {
            return stockUom;
        }

        public void setStockUom(String stockUom) {
            this.stockUom = stockUom;
        }

        public String getUom() {
            return uom;
        }

        public void setUom(String uom) {
            this.uom = uom;
        }

        public String getConversionFactor() {
            return conversionFactor;
        }

        public void setConversionFactor(String conversionFactor) {
            this.conversionFactor = conversionFactor;
        }

        public String getBaseRate() {
            return baseRate;
        }

        public void setBaseRate(String baseRate) {
            this.baseRate = baseRate;
        }

        public String getBaseAmount() {
            return baseAmount;
        }

        public void setBaseAmount(String baseAmount) {
            this.baseAmount = baseAmount;
        }

        public String getWarehouse() {
            return warehouse;
        }

        public void setWarehouse(String warehouse) {
            this.warehouse = warehouse;
        }
    }
}