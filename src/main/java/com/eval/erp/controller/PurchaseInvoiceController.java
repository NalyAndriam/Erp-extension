package com.eval.erp.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

@Controller
public class PurchaseInvoiceController {

    private static final Logger logger = LoggerFactory.getLogger(PurchaseInvoiceController.class);

    @Value("${erpnext.api.url}")
    private String erpNextApiUrl;

    private final HttpSession session;

    public PurchaseInvoiceController(HttpSession session) {
        this.session = session;
    }

    @GetMapping("/purchase-invoices")
    public String showPurchaseInvoices(Model model) {
        model.addAttribute("username", SecurityContextHolder.getContext().getAuthentication().getName());
        model.addAttribute("activeMenu", "invoices");

        String sid = (String) session.getAttribute("erp_sid");
        if (sid == null) {
            model.addAttribute("error", "ERPNext session not found. Please reconnect.");
            return "pages/purchase-invoices";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", sid);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();

        try {
            String fields = "[\"*\"]";
            String invoicesUrl = erpNextApiUrl + "/api/resource/Purchase Invoice?fields=" + fields;

            logger.info("Appel API : {}", invoicesUrl);

            ResponseEntity<Map> invoicesResponse = restTemplate.exchange(invoicesUrl, HttpMethod.GET, request, Map.class);
            List<Map<String, Object>> invoices = (List<Map<String, Object>>) invoicesResponse.getBody().get("data");
            model.addAttribute("invoices", invoices);
        } catch (HttpClientErrorException e) {
            logger.error("Erreur HTTP : {} - Réponse : {}", e.getStatusCode(), e.getResponseBodyAsString());
            model.addAttribute("error", "Erreur lors de la récupération des factures : " + e.getMessage());
        } catch (Exception e) {
            logger.error("Erreur inattendue : {}", e.getMessage());
            model.addAttribute("error", "Erreur lors de la récupération des factures : " + e.getMessage());
        }

        return "pages/purchase-invoices";
    }
}