package com.eval.erp.controller;

import com.eval.erp.model.PurchaseInvoiceDTO;
import com.eval.erp.model.PaymentEntryDTO;
import com.eval.erp.service.PurchaseInvoiceService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class PurchaseInvoiceController {

    private static final Logger logger = LoggerFactory.getLogger(PurchaseInvoiceController.class);

    private final PurchaseInvoiceService purchaseInvoiceService;
    private final HttpSession session;

    public PurchaseInvoiceController(PurchaseInvoiceService purchaseInvoiceService, HttpSession session) {
        this.purchaseInvoiceService = purchaseInvoiceService;
        this.session = session;
    }

    @GetMapping("/purchase-invoices")
    public String showPurchaseInvoices(Model model) {
        model.addAttribute("username", SecurityContextHolder.getContext().getAuthentication().getName());
        model.addAttribute("activeMenu", "invoices");

        try {
            List<PurchaseInvoiceDTO> invoices = purchaseInvoiceService.getPurchaseInvoices(session);
            model.addAttribute("invoices", invoices);
        } catch (IllegalStateException e) {
            logger.warn("Session error: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
        } catch (Exception e) {
            logger.error("Error retrieving invoices: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
        }

        return "pages/purchase-invoices";
    }

    @GetMapping("/invoice-details/{id}")
    public String showInvoiceDetails(@PathVariable String id, Model model) {
        model.addAttribute("username", SecurityContextHolder.getContext().getAuthentication().getName());
        model.addAttribute("activeMenu", "invoices");

        try {
            PurchaseInvoiceDTO invoice = purchaseInvoiceService.getInvoiceDetails(id, session);
            model.addAttribute("invoice", invoice);
            model.addAttribute("items", invoice.getItems() != null ? invoice.getItems() : new ArrayList<>());
        } catch (IllegalStateException e) {
            logger.warn("Session error: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
        } catch (Exception e) {
            logger.error("Error retrieving invoice details: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
        }

        return "pages/invoice-details";
    }

    @GetMapping("/payment-form/{id}")
    public String showPaymentForm(@PathVariable String id, Model model) {
        model.addAttribute("username", SecurityContextHolder.getContext().getAuthentication().getName());
        model.addAttribute("activeMenu", "invoices");

        try {
            Map<String, Object> formData = purchaseInvoiceService.getPaymentFormData(id, session);
            PurchaseInvoiceDTO invoice = (PurchaseInvoiceDTO) formData.get("invoice");
            model.addAttribute("invoiceId", invoice.getName());
            model.addAttribute("supplier", invoice.getSupplier());
            model.addAttribute("grand_total", invoice.getGrandTotal());
            model.addAttribute("outstanding_amount", invoice.getOutstandingAmount());
            model.addAttribute("due_date", invoice.getDueDate());
            model.addAttribute("modesOfPayment", formData.get("modesOfPayment"));
        } catch (IllegalStateException e) {
            logger.warn("Session error: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
        } catch (Exception e) {
            logger.error("Error retrieving payment form data: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
        }

        return "pages/payment-form";
    }

    @GetMapping("/create-payment/{id}")
    public String handleInvalidGetCreatePayment(@PathVariable String id, Model model) {
        logger.warn("Invalid GET request to /create-payment/{}", id);
        model.addAttribute("error", "Invalid request: GET is not supported for creating payments. Please use the payment form.");
        return "redirect:/payment-form/" + id;
    }

    @PostMapping(value = "/create-payment/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String createPayment(
            @PathVariable String id,
            @RequestParam String payment_type,
            @RequestParam String mode_of_payment,
            @RequestParam(required = false) Double paid_amount,
            @RequestParam(required = false) Double received_amount,
            @RequestParam String party_type,
            @RequestParam String party,
            @RequestParam(required = false) String posting_date,
            @RequestParam(required = false) Double allocated_amount,
            @RequestParam(required = false) String reference_no,
            @RequestParam(required = false) String reference_date,
            @RequestParam(required = false) Double source_exchange_rate,
            Model model) {
        model.addAttribute("username", SecurityContextHolder.getContext().getAuthentication().getName());
        model.addAttribute("activeMenu", "invoices");

        PaymentEntryDTO paymentEntry = new PaymentEntryDTO();
        paymentEntry.setInvoiceId(id);
        paymentEntry.setPaymentType(payment_type);
        paymentEntry.setModeOfPayment(mode_of_payment);
        paymentEntry.setPaidAmount(paid_amount);
        paymentEntry.setReceivedAmount(received_amount);
        paymentEntry.setPartyType(party_type);
        paymentEntry.setParty(party);
        paymentEntry.setPostingDate(posting_date);
        paymentEntry.setAllocatedAmount(allocated_amount);
        paymentEntry.setReferenceNo(reference_no);
        paymentEntry.setReferenceDate(reference_date);
        paymentEntry.setSourceExchangeRate(source_exchange_rate);

        try {
            purchaseInvoiceService.createPayment(paymentEntry, session);
            model.addAttribute("success", "Payment created and submitted successfully.");
            return "redirect:/purchase-invoices";
        } catch (IllegalStateException e) {
            logger.warn("Session error: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
        } catch (Exception e) {
            logger.error("Error creating payment: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
        }

        // Re-populate payment form on error
        try {
            Map<String, Object> formData = purchaseInvoiceService.getPaymentFormData(id, session);
            PurchaseInvoiceDTO invoice = (PurchaseInvoiceDTO) formData.get("invoice");
            model.addAttribute("invoiceId", invoice.getName());
            model.addAttribute("supplier", invoice.getSupplier());
            model.addAttribute("grand_total", invoice.getGrandTotal());
            model.addAttribute("outstanding_amount", invoice.getOutstandingAmount());
            model.addAttribute("due_date", invoice.getDueDate());
            model.addAttribute("modesOfPayment", formData.get("modesOfPayment"));
        } catch (Exception e) {
            logger.error("Error re-fetching payment form data: {}", e.getMessage());
            model.addAttribute("error", model.asMap().get("error") + "; Unable to reload form: " + e.getMessage());
        }

        return "pages/payment-form";
    }
}