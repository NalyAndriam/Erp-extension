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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import jakarta.servlet.http.HttpSession;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
}