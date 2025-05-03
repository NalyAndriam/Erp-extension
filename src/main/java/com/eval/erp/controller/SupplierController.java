package com.eval.erp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.eval.erp.model.QuotationUpdateDTO;

import jakarta.servlet.http.HttpSession;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class SupplierController {

    @Value("${erpnext.api.url}")
    private String erpNextApiUrl;

    private final HttpSession session;
    private static final Logger logger = LoggerFactory.getLogger(SupplierController.class);

    public SupplierController(HttpSession session) {
        this.session = session;
    }

    @GetMapping("/suppliers")
    public String showSuppliers(Model model) {
        // Récupérer le nom de l'utilisateur connecté
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        model.addAttribute("username", username);
        model.addAttribute("activeMenu", "suppliers"); // Pour la barre latérale

        // Récupérer le sid depuis la session
        String sid = (String) session.getAttribute("erp_sid");
        if (sid == null) {
            model.addAttribute("error", "ERPNext session not found. Please reconnect.");
            return "pages/suppliers";
        }

        // Appeler l'API ERPNext pour récupérer la liste des fournisseurs
        String suppliersUrl = erpNextApiUrl + "/api/resource/Supplier?fields=[\"*\"]";
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", sid);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                suppliersUrl, HttpMethod.GET, request, Map.class
            );
            List<Map<String, String>> suppliers = (List<Map<String, String>>) response.getBody().get("data");
            model.addAttribute("suppliers", suppliers);
        } catch (Exception e) {
            model.addAttribute("error", "Error retrieving suppliers: " + e.getMessage());
        }

        return "pages/suppliers";
    }

    // -------------------------------QUOTATIONS // DEMANDES DE DEVIS --------------------------------------------------------------

    @GetMapping("/quotations/{id}")
    public String showQuotationRequests(@PathVariable String id, Model model) {
        model.addAttribute("username", SecurityContextHolder.getContext().getAuthentication().getName());
        model.addAttribute("activeMenu", "suppliers");
        model.addAttribute("supplierName", id);

        String sid = (String) session.getAttribute("erp_sid");
        if (sid == null) {
            model.addAttribute("error", "ERPNext session not found. Please reconnect.");
            return "pages/quotation-requests";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", sid);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();

        try {
            String fields = "[\"*\"]";
            String filters = "[[\"supplier\", \"=\", \"" + id + "\"]]";
            String quotationsUrl = erpNextApiUrl + "/api/resource/Supplier Quotation?fields=" + fields + "&filters=" + filters;

            logger.info("Appel API : {}", quotationsUrl);

            ResponseEntity<Map> quotationsResponse = restTemplate.exchange(quotationsUrl, HttpMethod.GET, request, Map.class);
            List<Map<String, Object>> quotations = (List<Map<String, Object>>) quotationsResponse.getBody().get("data");
            model.addAttribute("quotations", quotations);
        } catch (HttpClientErrorException e) {
            logger.error("Erreur HTTP : {} - Réponse : {}", e.getStatusCode(), e.getResponseBodyAsString());
            model.addAttribute("error", "Erreur lors de la récupération des devis : " + e.getMessage());
        } catch (Exception e) {
            logger.error("Erreur inattendue : {}", e.getMessage());
            model.addAttribute("error", "Erreur lors de la récupération des devis : " + e.getMessage());
        }

        return "pages/quotation-requests";
    }

    @GetMapping("/quotation-details/{id}")
    public String showQuotationDetail(@PathVariable String id, Model model) {
        model.addAttribute("username", SecurityContextHolder.getContext().getAuthentication().getName());
        model.addAttribute("activeMenu", "suppliers");

        String sid = (String) session.getAttribute("erp_sid");
        if (sid == null) {
            model.addAttribute("error", "ERPNext session not found. Please reconnect.");
            return "pages/quotation-details";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", sid);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();

        try {
            String detailUrl = erpNextApiUrl + "/api/resource/Supplier Quotation/" + URLEncoder.encode(id, StandardCharsets.UTF_8) +
                    "?fields=[\"name\",\"title\",\"status\",\"supplier\",\"transaction_date\",\"grand_total\",\"items.name\",\"items.item_code\",\"items.item_name\",\"items.qty\",\"items.rate\",\"items.amount\",\"items.stock_uom\",\"items.uom\",\"items.conversion_factor\",\"items.base_rate\",\"items.base_amount\",\"items.warehouse\"]";
            ResponseEntity<Map> response = restTemplate.exchange(detailUrl, HttpMethod.GET, request, Map.class);
            Map<String, Object> quotation = (Map<String, Object>) response.getBody().get("data");
            model.addAttribute("quotation", quotation);
            logger.info("Quotation data: {}", quotation);
        } catch (Exception e) {
            logger.error("Error retrieving quotation: {}", e.getMessage());
            model.addAttribute("error", "Erreur lors de la récupération du devis : " + e.getMessage());
        }

        return "pages/quotation-details";
    }

    @PostMapping("/quotation-details/save")
    public String saveQuotation(@ModelAttribute QuotationUpdateDTO formData, Model model, RedirectAttributes redirectAttributes) {
        String sid = (String) session.getAttribute("erp_sid");
        if (sid == null) {
            model.addAttribute("error", "ERPNext session not found. Please reconnect.");
            logger.error("No ERPNext session found (sid is null)");
            return "redirect:/suppliers";
        }

        String name = formData.getName();
        List<QuotationUpdateDTO.ItemUpdateDTO> items = formData.getItems();

        // Validation des données de base
        if (name == null || items == null) {
            model.addAttribute("error", "Invalid input data.");
            logger.error("Invalid input data: name={}, items={}", name, items);
            return "redirect:/quotation-details/" + URLEncoder.encode(name != null ? name : "", StandardCharsets.UTF_8);
        }

        // Vérifier le statut de la quotation
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", sid);
        HttpEntity<Void> statusRequest = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();
        try {
            String statusUrl = erpNextApiUrl + "/api/resource/Supplier Quotation/" + URLEncoder.encode(name, StandardCharsets.UTF_8) + "?fields=[\"status\",\"docstatus\"]";
            ResponseEntity<Map> statusResponse = restTemplate.exchange(statusUrl, HttpMethod.GET, statusRequest, Map.class);
            Map<String, Object> quotationStatus = (Map<String, Object>) statusResponse.getBody().get("data");
            Integer docstatus = (Integer) quotationStatus.get("docstatus");
            String status = (String) quotationStatus.get("status");

            if (docstatus != null && docstatus == 1) {
                redirectAttributes.addFlashAttribute("error", "Cannot update a submitted quotation.");
                logger.error("Quotation {} is submitted (docstatus=1, status={})", name, status);
                return "redirect:/quotation-details/" + URLEncoder.encode(name, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            logger.error("Error checking quotation status: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error checking quotation status: " + e.getMessage());
            return "redirect:/quotation-details/" + URLEncoder.encode(name, StandardCharsets.UTF_8);
        }

        // Log des données reçues
        logger.info("Received form data: name={}, items={}", name, items);

        // Construire la payload pour l'API
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

            // Log des données de l'item
            logger.info("Item data: name={}, itemName={}, itemCode={}, rate={}, qty={}, stockUom={}, uom={}, conversionFactor={}, baseRate={}, baseAmount={}, warehouse={}",
                    itemName, itemNameField, itemCode, rate, qty, stockUom, uom, conversionFactor, baseRate, baseAmount, warehouse);

            if (itemName == null || rate == null || itemCode == null || itemNameField == null || qty == null ||
                    stockUom == null || uom == null || conversionFactor == null ||
                    baseRate == null || baseAmount == null || warehouse == null || !rate.matches("\\d+(\\.\\d{1,2})?")) {
                redirectAttributes.addFlashAttribute("error", "Invalid data for item: " + itemName);
                logger.error("Invalid data for item: itemName={}, rate={}, itemCode={}, itemNameField={}, qty={}, stockUom={}, uom={}, conversionFactor={}, baseRate={}, baseAmount={}, warehouse={}",
                        itemName, rate, itemCode, itemNameField, qty, stockUom, uom, conversionFactor, baseRate, baseAmount, warehouse);
                return "redirect:/quotation-details/" + URLEncoder.encode(name, StandardCharsets.UTF_8);
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

        // Créer le payload pour l'API
        Map<String, Object> payload = new HashMap<>();
        payload.put("items", updatedItems);
        logger.info("Payload sent to ERPNext: {}", payload);

        headers.add("Content-Type", "application/json");
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            String updateUrl = erpNextApiUrl + "/api/resource/Supplier Quotation/" + URLEncoder.encode(name, StandardCharsets.UTF_8);
            logger.info("Sending PUT request to: {}", updateUrl);
            ResponseEntity<Map> response = restTemplate.exchange(updateUrl, HttpMethod.PUT, request, Map.class);
            logger.info("API Response: {}", response.getBody());
            redirectAttributes.addFlashAttribute("success", "Quotation updated successfully.");
        } catch (HttpClientErrorException e) {
            logger.error("HTTP Error: {} - Response: {}", e.getStatusCode(), e.getResponseBodyAsString());
            redirectAttributes.addFlashAttribute("error", "Error updating quotation: " + e.getMessage());
            return "redirect:/quotation-details/" + URLEncoder.encode(name, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error updating quotation: " + e.getMessage());
            return "redirect:/quotation-details/" + URLEncoder.encode(name, StandardCharsets.UTF_8);
        }

        return "redirect:/quotation-details/" + URLEncoder.encode(name, StandardCharsets.UTF_8) + "?t=" + System.currentTimeMillis();
    }


    // -------------------------------PURCHASE ORDERS - LISTE COMMANDES --------------------------------------------------------------

    @GetMapping("/purchase-orders/{id}")
    public String showPurchaseOrders(@PathVariable String id, Model model) {
        model.addAttribute("username", SecurityContextHolder.getContext().getAuthentication().getName());
        model.addAttribute("activeMenu", "suppliers");
        model.addAttribute("supplierName", id);

        String sid = (String) session.getAttribute("erp_sid");
        if (sid == null) {
            model.addAttribute("error", "ERPNext session not found. Please reconnect.");
            return "pages/purchase-orders";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", sid);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();

        try {
            String fields = "[\"*\"]";
            String filters = "[[\"supplier\", \"=\", \"" + id + "\"]]";
            String purchaseOrdersUrl = erpNextApiUrl + "/api/resource/Purchase Order?fields=" + fields + "&filters=" + filters;

            logger.info("Appel API : {}", purchaseOrdersUrl);

            ResponseEntity<Map> purchaseOrdersResponse = restTemplate.exchange(purchaseOrdersUrl, HttpMethod.GET, request, Map.class);
            List<Map<String, Object>> purchaseOrders = (List<Map<String, Object>>) purchaseOrdersResponse.getBody().get("data");
            model.addAttribute("purchaseOrders", purchaseOrders);
        } catch (HttpClientErrorException e) {
            logger.error("Erreur HTTP : {} - Réponse : {}", e.getStatusCode(), e.getResponseBodyAsString());
            model.addAttribute("error", "Erreur lors de la récupération des commandes : " + e.getMessage());
        } catch (Exception e) {
            logger.error("Erreur inattendue : {}", e.getMessage());
            model.addAttribute("error", "Erreur lors de la récupération des commandes : " + e.getMessage());
        }

        return "pages/purchase-orders";
    }



}