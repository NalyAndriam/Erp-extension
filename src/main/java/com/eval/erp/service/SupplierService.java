package com.eval.erp.service;

import com.eval.erp.model.PurchaseOrderDTO;
import com.eval.erp.model.QuotationUpdateDTO;
import com.eval.erp.model.SupplierDTO;
import com.eval.erp.model.SupplierQuotationDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SupplierService {

    private static final Logger logger = LoggerFactory.getLogger(SupplierService.class);

    private final ErpNextApiService apiService;

    public SupplierService(ErpNextApiService apiService) {
        this.apiService = apiService;
    }

    public List<SupplierDTO> getSuppliers(String sid) throws Exception {
        try {
            String fields = "[\"*\"]";
            ResponseEntity<Map> response = apiService.getResource("Supplier", fields, null, sid);
            List<Map<String, Object>> rawSuppliers = (List<Map<String, Object>>) response.getBody().get("data");
            return convertToSupplierDTOs(rawSuppliers);
        } catch (Exception e) {
            logger.error("Error retrieving suppliers: {}", e.getMessage());
            throw new Exception("Failed to retrieve suppliers: " + e.getMessage(), e);
        }
    }

    public List<SupplierQuotationDTO> getQuotationRequests(String supplierId, String sid) throws Exception {
        try {
            String fields = "[\"*\"]";
            String filters = "[[\"supplier\", \"=\", \"" + supplierId + "\"]]";
            ResponseEntity<Map> response = apiService.getResource("Supplier Quotation", fields, filters, sid);
            List<Map<String, Object>> rawQuotations = (List<Map<String, Object>>) response.getBody().get("data");
            return convertToSupplierQuotationDTOs(rawQuotations);
        } catch (HttpClientErrorException e) {
            logger.error("HTTP error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new Exception("Failed to retrieve quotations: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage());
            throw new Exception("Failed to retrieve quotations: " + e.getMessage(), e);
        }
    }

    public SupplierQuotationDTO getQuotationDetail(String id, String sid) throws Exception {
        try {
            String fields = "[\"name\",\"title\",\"status\",\"supplier\",\"transaction_date\",\"grand_total\",\"items.name\",\"items.item_code\",\"items.item_name\",\"items.qty\",\"items.rate\",\"items.amount\",\"items.stock_uom\",\"items.uom\",\"items.conversion_factor\",\"items.base_rate\",\"items.base_amount\",\"items.warehouse\"]";
            ResponseEntity<Map> response = apiService.getResourceById("Supplier Quotation", URLEncoder.encode(id, StandardCharsets.UTF_8), fields, sid);
            Map<String, Object> quotationData = (Map<String, Object>) response.getBody().get("data");
            SupplierQuotationDTO quotation = convertToSupplierQuotationDTO(quotationData);
            logger.info("Quotation data: {}", quotation);
            return quotation;
        } catch (Exception e) {
            logger.error("Error retrieving quotation: {}", e.getMessage());
            throw new Exception("Failed to retrieve quotation: " + e.getMessage(), e);
        }
    }

    public void updateQuotation(QuotationUpdateDTO formData, String sid) throws Exception {
        String name = formData.getName();
        List<QuotationUpdateDTO.ItemUpdateDTO> items = formData.getItems();
    
        if (name == null || items == null) {
            logger.error("Invalid input data: name={}, items={}", name, items);
            throw new Exception("Invalid input data: name or items are null");
        }
    
        // Check quotation status
        try {
            String fields = "[\"status\",\"docstatus\"]";
            ResponseEntity<Map> statusResponse = apiService.getResourceById("Supplier Quotation", URLEncoder.encode(name, StandardCharsets.UTF_8), fields, sid);
            Map<String, Object> quotationStatus = (Map<String, Object>) statusResponse.getBody().get("data");
            Integer docstatus = (Integer) quotationStatus.get("docstatus");
            String status = (String) quotationStatus.get("status");
    
            if (docstatus != null && docstatus == 1) {
                logger.error("Quotation {} is submitted (docstatus=1, status={})", name, status);
                throw new Exception("Cannot update a submitted quotation");
            }
        } catch (Exception e) {
            logger.error("Error checking quotation status: {}", e.getMessage());
            throw new Exception("Failed to check quotation status: " + e.getMessage(), e);
        }
    
        logger.info("Processing form data: name={}, items={}", name, items);
    
        // Validate and prepare items
        List<Map<String, Object>> updatedItems = new ArrayList<>();
        for (QuotationUpdateDTO.ItemUpdateDTO item : items) {
            String itemName = item.getName();
            String rate = item.getRate();
            String itemCode = item.getItemCode();
            String itemNameField = item.getItemName();
            String qty = item.getQty();
            String stockUom = item.getStockUom();
            String uom = item.getUom();
            String conversionFactor = item.getConversionFactor();
            String baseRate = item.getBaseRate();
            String baseAmount = item.getBaseAmount();
            String warehouse = item.getWarehouse();
    
            logger.info("Item data: name={}, itemName={}, itemCode={}, rate={}, qty={}, stockUom={}, uom={}, conversionFactor={}, baseRate={}, baseAmount={}, warehouse={}",
                    itemName, itemNameField, itemCode, rate, qty, stockUom, uom, conversionFactor, baseRate, baseAmount, warehouse);
    
            if (itemName == null || rate == null || itemCode == null || itemNameField == null || qty == null ||
                    stockUom == null || uom == null || conversionFactor == null ||
                    baseRate == null || baseAmount == null || warehouse == null || !rate.matches("\\d+(\\.\\d{1,2})?")) {
                logger.error("Invalid data for item: itemName={}, rate={}, itemCode={}, itemNameField={}, qty={}, stockUom={}, uom={}, conversionFactor={}, baseRate={}, baseAmount={}, warehouse={}",
                        itemName, rate, itemCode, itemNameField, qty, stockUom, uom, conversionFactor, baseRate, baseAmount, warehouse);
                throw new Exception("Invalid data for item: " + itemName);
            }
    
            Map<String, Object> updatedItem = new HashMap<>();
            updatedItem.put("name", itemName);
            updatedItem.put("item_code", itemCode);
            updatedItem.put("item_name", itemNameField);
            updatedItem.put("qty", Double.parseDouble(qty));
            updatedItem.put("rate", Double.parseDouble(rate));
            updatedItem.put("stock_uom", stockUom);
            updatedItem.put("uom", uom);
            updatedItem.put("conversion_factor", Double.parseDouble(conversionFactor));
            updatedItem.put("base_rate", Double.parseDouble(baseRate));
            updatedItem.put("base_amount", Double.parseDouble(baseAmount));
            updatedItem.put("warehouse", warehouse);
            updatedItems.add(updatedItem);
        }
    
        // Prepare payload and update
        Map<String, Object> payload = new HashMap<>();
        payload.put("items", updatedItems);
        logger.info("Payload sent to ERPNext: {}", payload);
    
        try {
            // Update the quotation
            ResponseEntity<Map> response = apiService.updateResource("Supplier Quotation", URLEncoder.encode(name, StandardCharsets.UTF_8), payload, sid);
            logger.info("API Response: {}", response.getBody());
    
            // Submit the quotation
            ResponseEntity<Map> submitResponse = apiService.submitResource("Supplier Quotation", URLEncoder.encode(name, StandardCharsets.UTF_8), sid);
            logger.info("Quotation submitted successfully: {}", submitResponse.getBody());
        } catch (HttpClientErrorException e) {
            logger.error("HTTP Error: {} - Response: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new Exception("Failed to update or submit quotation: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage(), e);
            throw new Exception("Failed to update or submit quotation: " + e.getMessage(), e);
        }
    }

    public List<PurchaseOrderDTO> getPurchaseOrders(String supplierId, String sid) throws Exception {
        try {
            String fields = "[\"name\", \"supplier\", \"supplier_name\", \"status\", \"transaction_date\", \"grand_total\", \"per_billed\", \"per_received\"]";
            String filters = "[[\"supplier\", \"=\", \"" + supplierId + "\"]]";
            ResponseEntity<Map> response = apiService.getResource("Purchase Order", fields, filters, sid);
            List<Map<String, Object>> rawPurchaseOrders = (List<Map<String, Object>>) response.getBody().get("data");
            return convertToPurchaseOrderDTOs(rawPurchaseOrders);
        } catch (HttpClientErrorException e) {
            logger.error("HTTP error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new Exception("Failed to retrieve purchase orders: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage());
            throw new Exception("Failed to retrieve purchase orders: " + e.getMessage(), e);
        }
    }

    private List<SupplierDTO> convertToSupplierDTOs(List<Map<String, Object>> rawSuppliers) {
        List<SupplierDTO> suppliers = new ArrayList<>();
        for (Map<String, Object> raw : rawSuppliers) {
            SupplierDTO dto = new SupplierDTO();
            dto.setName((String) raw.get("name"));
            dto.setSupplierName((String) raw.get("supplier_name"));
            dto.setSupplierGroup((String) raw.get("supplier_group"));
            dto.setDisabled(String.valueOf(raw.get("disabled")));
            suppliers.add(dto);
        }
        return suppliers;
    }

    private List<SupplierQuotationDTO> convertToSupplierQuotationDTOs(List<Map<String, Object>> rawQuotations) {
        List<SupplierQuotationDTO> quotations = new ArrayList<>();
        for (Map<String, Object> raw : rawQuotations) {
            quotations.add(convertToSupplierQuotationDTO(raw));
        }
        return quotations;
    }

    private SupplierQuotationDTO convertToSupplierQuotationDTO(Map<String, Object> raw) {
        SupplierQuotationDTO dto = new SupplierQuotationDTO();
        dto.setName((String) raw.get("name"));
        dto.setTitle((String) raw.get("title"));
        dto.setStatus((String) raw.get("status"));
        dto.setSupplier((String) raw.get("supplier"));
        dto.setTransactionDate((String) raw.get("transaction_date"));
        dto.setGrandTotal(raw.get("grand_total") != null ? ((Number) raw.get("grand_total")).doubleValue() : null);
        dto.setItems((List<Map<String, Object>>) raw.get("items"));
        dto.setDocstatus((Integer) raw.get("docstatus"));
        return dto;
    }

    private List<PurchaseOrderDTO> convertToPurchaseOrderDTOs(List<Map<String, Object>> rawPurchaseOrders) {
        List<PurchaseOrderDTO> purchaseOrders = new ArrayList<>();
        for (Map<String, Object> raw : rawPurchaseOrders) {
            PurchaseOrderDTO dto = new PurchaseOrderDTO();
            dto.setName((String) raw.get("name"));
            dto.setSupplier((String) raw.get("supplier"));
            dto.setSupplierName((String) raw.get("supplier_name"));
            dto.setStatus((String) raw.get("status"));
            dto.setTransactionDate((String) raw.get("transaction_date"));
            dto.setGrandTotal(raw.get("grand_total") != null ? ((Number) raw.get("grand_total")).doubleValue() : null);
            dto.setPerBilled(raw.get("per_billed") != null ? ((Number) raw.get("per_billed")).doubleValue() : null);
            dto.setPerReceived(raw.get("per_received") != null ? ((Number) raw.get("per_received")).doubleValue() : null);
            purchaseOrders.add(dto);
        }
        return purchaseOrders;
    }
}