package com.eval.erp.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import jakarta.servlet.http.HttpSession;

@Controller
public class LoginController {

    @Value("${erpnext.api.url}")
    private String erpNextApiUrl;

    @GetMapping("/")
    public String redirectToLogin() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String showLoginPage(@RequestParam(required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("error", "Échec de la connexion : identifiants invalides");
        }
        return "login";
    }


   @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {

        String erpUrl = erpNextApiUrl + "/api/method/login";

        RestTemplate restTemplate = new RestTemplate();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("usr", username);
        formData.add("pwd", password);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                erpUrl, HttpMethod.POST, request, String.class
            );

            // Récupérer le cookie de session ERPNext
            List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);

            System.out.println("Cookies reçus : " + cookies);

            String sid = cookies != null ? cookies.stream()
                .filter(c -> c.startsWith("sid="))
                .findFirst().orElse(null) : null;

            

            if (sid == null) {
                System.out.println("NULL");
                redirectAttributes.addFlashAttribute("error", "Échec de la connexion : Pas de session ERPNext");
                return "redirect:/login";
            }

            System.out.println("TSY NULL");
            // Sauvegarde du SID dans la session Spring Boot
            session.setAttribute("erp_sid", sid);

            // Rediriger vers la page d'accueil ou dashboard
            return "redirect:/home";

        } catch (HttpClientErrorException e) {
            // Remplace dans le bloc catch :
            redirectAttributes.addFlashAttribute("error", "Échec de la connexion : identifiants invalides");
            return "redirect:/login";
        }
    }
    
}