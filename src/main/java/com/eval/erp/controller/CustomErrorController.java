package com.eval.erp.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute("javax.servlet.error.status_code");
        Object exception = request.getAttribute("javax.servlet.error.exception");

        String errorMessage = "Une erreur inattendue s'est produite";
        if (exception != null) {
            errorMessage = exception.toString();
        } else if (status != null) {
            errorMessage = "Erreur HTTP : " + status;
        }

        model.addAttribute("errorMessage", errorMessage);
        return "error";
    }
}