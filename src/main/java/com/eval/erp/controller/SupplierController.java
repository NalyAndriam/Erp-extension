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
import org.springframework.web.client.RestTemplate;
import jakarta.servlet.http.HttpSession;

import java.util.List;
import java.util.Map;

@Controller
public class SupplierController {

    @Value("${erpnext.api.url}")
    private String erpNextApiUrl;

    private final HttpSession session;

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
            model.addAttribute("error", "Session ERPNext non trouvée. Veuillez vous reconnecter.");
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
            model.addAttribute("error", "Erreur lors de la récupération des fournisseurs : " + e.getMessage());
        }

        return "pages/suppliers";
    }
}