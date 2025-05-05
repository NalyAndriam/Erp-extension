package com.eval.erp.controller;

import com.eval.erp.model.QuotationUpdateDTO;
import com.eval.erp.service.SupplierService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
public class SupplierController {

    private final SupplierService supplierService;
    private final HttpSession session;

    public SupplierController(SupplierService supplierService, HttpSession session) {
        this.supplierService = supplierService;
        this.session = session;
    }

    @GetMapping("/suppliers")
    public String showSuppliers(Model model) {
        model.addAttribute("username", SecurityContextHolder.getContext().getAuthentication().getName());
        model.addAttribute("activeMenu", "suppliers");

        String sid = (String) session.getAttribute("erp_sid");
        if (sid == null) {
            model.addAttribute("error", "ERPNext session not found. Please reconnect.");
            return "pages/suppliers";
        }

        try {
            model.addAttribute("suppliers", supplierService.getSuppliers(sid));
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
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

        try {
            model.addAttribute("quotations", supplierService.getQuotationRequests(id, sid));
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
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

        try {
            model.addAttribute("quotation", supplierService.getQuotationDetail(id, sid));
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }

        return "pages/quotation-details";
    }

    @PostMapping("/quotation-details/save")
    public String saveQuotation(@ModelAttribute QuotationUpdateDTO formData, Model model, RedirectAttributes redirectAttributes) {
        String sid = (String) session.getAttribute("erp_sid");
        if (sid == null) {
            redirectAttributes.addFlashAttribute("error", "ERPNext session not found. Please reconnect.");
            return "redirect:/suppliers";
        }

        String name = formData.getName();
        try {
            supplierService.updateQuotation(formData, sid);
            redirectAttributes.addFlashAttribute("success", "Quotation updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/quotation-details/" + URLEncoder.encode(name != null ? name : "", StandardCharsets.UTF_8) + "?t=" + System.currentTimeMillis();
    }

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

        try {
            model.addAttribute("purchaseOrders", supplierService.getPurchaseOrders(id, sid));
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }

        return "pages/purchase-orders";
    }
}