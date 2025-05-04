package com.eval.erp.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class PurchaseInvoiceController {

    private static final Logger logger = LoggerFactory.getLogger(PurchaseInvoiceController.class);

    @Value("${erpnext.api.url}")
    private String erpNextApiUrl;

    @Value("${erpnext.default_payment_account}")
    private String defaultPaymentAccount;

    @Value("${erpnext.default_payment_account_currency}")
    private String defaultPaymentAccountCurrency;

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

    @GetMapping("/payment-form/{id}")
    public String showPaymentForm(@PathVariable String id, Model model) {
        model.addAttribute("username", SecurityContextHolder.getContext().getAuthentication().getName());
        model.addAttribute("activeMenu", "invoices");

        String sid = (String) session.getAttribute("erp_sid");
        if (sid == null) {
            model.addAttribute("error", "ERPNext session not found. Please reconnect.");
            return "pages/payment-form";
        }

        return fetchPaymentFormData(id, model, sid);
    }

    private String fetchPaymentFormData(String id, Model model, String sid) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", sid);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();
    
        try {
            // Récupérer les détails de la facture
            String invoiceUrl = erpNextApiUrl + "/api/resource/Purchase Invoice/" + id;
            ResponseEntity<Map> invoiceResponse = restTemplate.exchange(invoiceUrl, HttpMethod.GET, request, Map.class);
            Map<String, Object> invoiceData = (Map<String, Object>) invoiceResponse.getBody().get("data");
    
            if (invoiceData == null || (!"Unpaid".equals(invoiceData.get("status")) && !"Partially Paid".equals(invoiceData.get("status")))) {
                model.addAttribute("error", "Invoice not found or not in Unpaid/Partially Paid status.");
                return "pages/payment-form";
            }
    
            // Ajouter les données de la facture au modèle
            model.addAttribute("invoiceId", id);
            model.addAttribute("supplier", invoiceData.get("supplier"));
            model.addAttribute("grand_total", invoiceData.get("grand_total"));
            model.addAttribute("outstanding_amount", invoiceData.get("outstanding_amount"));
            model.addAttribute("due_date", invoiceData.get("due_date"));
    
            // Récupérer les modes de paiement
            String modesUrl = erpNextApiUrl + "/api/resource/Mode of Payment?fields=[\"name\"]";
            ResponseEntity<Map> modesResponse = restTemplate.exchange(modesUrl, HttpMethod.GET, request, Map.class);
            List<Map<String, Object>> modesOfPayment = (List<Map<String, Object>>) modesResponse.getBody().get("data");
            model.addAttribute("modesOfPayment", modesOfPayment);
    
        } catch (HttpClientErrorException e) {
            logger.error("HTTP error fetching data for payment form: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            model.addAttribute("error", "Error fetching data: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage());
            model.addAttribute("error", "Unexpected error: " + e.getMessage());
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
        logger.info("Received POST request for /create-payment/{}", id);
        String sid = (String) session.getAttribute("erp_sid");
        if (sid == null) {
            model.addAttribute("error", "ERPNext session not found. Please reconnect.");
            return fetchPaymentFormData(id, model, sid);
        }
    
        // Validate required fields
        if (mode_of_payment == null || mode_of_payment.trim().isEmpty()) {
            model.addAttribute("error", "Mode of Payment is required.");
            return fetchPaymentFormData(id, model, sid);
        }
        if (source_exchange_rate == null || source_exchange_rate <= 0) {
            source_exchange_rate = 1.0;
        }
    
        // Fetch invoice details to validate payment amount
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", sid);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();
    
        try {
            String invoiceUrl = erpNextApiUrl + "/api/resource/Purchase Invoice/" + id + "?fields=[\"name\",\"outstanding_amount\",\"company\",\"credit_to\",\"currency\"]";
            ResponseEntity<Map> invoiceResponse = restTemplate.exchange(invoiceUrl, HttpMethod.GET, request, Map.class);
            Map<String, Object> invoiceData = (Map<String, Object>) invoiceResponse.getBody().get("data");
    
            if (invoiceData == null) {
                model.addAttribute("error", "Invoice not found.");
                return fetchPaymentFormData(id, model, sid);
            }
    
            Double outstandingAmount = ((Number) invoiceData.get("outstanding_amount")).doubleValue();
            String company = (String) invoiceData.get("company");
            String creditTo = (String) invoiceData.get("credit_to");
            String invoiceCurrency = (String) invoiceData.get("currency");
    
            // Validate paid_amount
            if (paid_amount == null || paid_amount <= 0) {
                model.addAttribute("error", "Paid Amount must be greater than 0.");
                return fetchPaymentFormData(id, model, sid);
            }
            if (paid_amount > outstandingAmount) {
                model.addAttribute("error", "Paid Amount cannot exceed Outstanding Amount (" + outstandingAmount + ").");
                return fetchPaymentFormData(id, model, sid);
            }
    
            // Set allocated_amount to paid_amount
            allocated_amount = paid_amount;
    
            // Validate received_amount
            if (received_amount != null && Math.abs(received_amount - paid_amount) > 0.01) {
                model.addAttribute("error", "Received Amount must equal Paid Amount.");
                return fetchPaymentFormData(id, model, sid);
            }
    
            // Validate credit_to
            if (creditTo == null || creditTo.trim().isEmpty()) {
                model.addAttribute("error", "No creditor account associated with the invoice.");
                return fetchPaymentFormData(id, model, sid);
            }
    
            // Prepare the request body for creating a Payment Entry
            Map<String, Object> paymentData = new HashMap<>();
            paymentData.put("doctype", "Payment Entry");
            paymentData.put("payment_type", payment_type);
            paymentData.put("mode_of_payment", mode_of_payment);
            paymentData.put("party_type", party_type);
            paymentData.put("party", party);
            paymentData.put("source_exchange_rate", source_exchange_rate);
            paymentData.put("paid_from", defaultPaymentAccount);
            paymentData.put("paid_from_account_currency", defaultPaymentAccountCurrency);
            paymentData.put("company", company);
            paymentData.put("paid_to", creditTo);
            paymentData.put("paid_to_account_currency", invoiceCurrency);
            if (posting_date != null && !posting_date.trim().isEmpty()) {
                paymentData.put("posting_date", posting_date);
            } else {
                paymentData.put("posting_date", java.time.LocalDate.now().toString());
            }
            if (reference_no != null && !reference_no.trim().isEmpty()) {
                paymentData.put("reference_no", reference_no);
            }
            if (reference_date != null && !reference_date.trim().isEmpty()) {
                paymentData.put("reference_date", reference_date);
            }
            paymentData.put("paid_amount", paid_amount);
            paymentData.put("received_amount", paid_amount);
            paymentData.put("base_paid_amount", paid_amount); // Ajout pour éviter d'autres erreurs
            paymentData.put("base_received_amount", paid_amount); // Ajout pour éviter d'autres erreurs
    
            // Add reference to the invoice
            List<Map<String, Object>> references = new ArrayList<>();
            Map<String, Object> reference = new HashMap<>();
            reference.put("reference_doctype", "Purchase Invoice");
            reference.put("reference_name", id);
            reference.put("allocated_amount", allocated_amount);
            references.add(reference);
            paymentData.put("references", references);
    
            // Create the Payment Entry
            ResponseEntity<Map> createResponse = submitPaymentEntry(paymentData, headers, id, sid, model);
            if (createResponse == null) {
                return fetchPaymentFormData(id, model, sid);
            }
    
            // Extract the Payment Entry name from the response
            Map<String, Object> createResponseData = (Map<String, Object>) createResponse.getBody().get("data");
            String paymentEntryName = (String) createResponseData.get("name");
            if (paymentEntryName == null || paymentEntryName.trim().isEmpty()) {
                model.addAttribute("error", "Failed to retrieve Payment Entry name after creation.");
                return fetchPaymentFormData(id, model, sid);
            }
    
            // Fetch the latest Payment Entry to avoid TimestampMismatchError
            String paymentEntryUrl = erpNextApiUrl + "/api/resource/Payment Entry/" + paymentEntryName;
            ResponseEntity<Map> paymentEntryResponse = restTemplate.exchange(paymentEntryUrl, HttpMethod.GET, request, Map.class);
            Map<String, Object> paymentEntryData = (Map<String, Object>) paymentEntryResponse.getBody().get("data");
            if (paymentEntryData == null) {
                model.addAttribute("error", "Failed to retrieve Payment Entry after creation.");
                return fetchPaymentFormData(id, model, sid);
            }
    
            // Submit the Payment Entry
            ResponseEntity<Map> submitResponse = submitPaymentEntryDoc(paymentEntryData, headers, id, sid, model);
            if (submitResponse == null) {
                return fetchPaymentFormData(id, model, sid);
            }
    
            logger.info("Payment Entry {} submitted successfully for invoice {}", paymentEntryName, id);
            model.addAttribute("success", "Payment created and submitted successfully.");
            return "redirect:/purchase-invoices";
    
        } catch (HttpClientErrorException e) {
            logger.error("HTTP error fetching invoice {}: StatusCode={}, ResponseBody={}", 
                         id, e.getStatusCode(), e.getResponseBodyAsString());
            model.addAttribute("error", "Error fetching invoice: " + e.getMessage() + " - " + e.getResponseBodyAsString());
            return fetchPaymentFormData(id, model, sid);
        } catch (Exception e) {
            logger.error("Unexpected error fetching invoice {}: {}", id, e.getMessage(), e);
            model.addAttribute("error", "Unexpected error: " + e.getMessage());
            return fetchPaymentFormData(id, model, sid);
        }
    }
    
    private ResponseEntity<Map> submitPaymentEntry(Map<String, Object> paymentData, HttpHeaders headers, String invoiceId, String sid, Model model) {
        RestTemplate restTemplate = new RestTemplate();
        String paymentUrl = erpNextApiUrl + "/api/resource/Payment Entry";
    
        try {
            logger.info("Submitting Payment Entry for invoice {}: Data={}", invoiceId, paymentData);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> paymentRequest = new HttpEntity<>(paymentData, headers);
            ResponseEntity<Map> response = restTemplate.exchange(paymentUrl, HttpMethod.POST, paymentRequest, Map.class);
            logger.info("Payment Entry response for invoice {}: StatusCode={}, Body={}", 
                        invoiceId, response.getStatusCode(), response.getBody());
            return response;
        } catch (HttpClientErrorException e) {
            logger.error("HTTP error submitting Payment Entry for invoice {}: StatusCode={}, ResponseBody={}", 
                         invoiceId, e.getStatusCode(), e.getResponseBodyAsString());
            model.addAttribute("error", "Error creating payment: " + e.getMessage() + " - " + e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            logger.error("Unexpected error submitting Payment Entry for invoice {}: {}", invoiceId, e.getMessage(), e);
            model.addAttribute("error", "Unexpected error creating payment: " + e.getMessage());
            return null;
        }
    }
    
    private ResponseEntity<Map> submitPaymentEntryDoc(Map<String, Object> paymentEntryData, HttpHeaders headers, String invoiceId, String sid, Model model) {
        RestTemplate restTemplate = new RestTemplate();
        String submitUrl = erpNextApiUrl + "/api/method/frappe.client.submit";
    
        try {
            // Prepare the request body for submitting the Payment Entry
            Map<String, Object> submitData = new HashMap<>();
            submitData.put("doc", paymentEntryData); // Utiliser les données complètes du document
    
            logger.info("Submitting Payment Entry document for invoice {}: Data={}", invoiceId, submitData);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> submitRequest = new HttpEntity<>(submitData, headers);
            ResponseEntity<Map> response = restTemplate.exchange(submitUrl, HttpMethod.POST, submitRequest, Map.class);
            logger.info("Submit Payment Entry response for invoice {}: StatusCode={}, Body={}", 
                        invoiceId, response.getStatusCode(), response.getBody());
            return response;
        } catch (HttpClientErrorException e) {
            logger.error("HTTP error submitting Payment Entry document for invoice {}: StatusCode={}, ResponseBody={}", 
                         invoiceId, e.getStatusCode(), e.getResponseBodyAsString());
            model.addAttribute("error", "Error submitting payment: " + e.getMessage() + " - " + e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            logger.error("Unexpected error submitting Payment Entry document for invoice {}: {}", 
                         invoiceId, e.getMessage(), e);
            model.addAttribute("error", "Unexpected error submitting payment: " + e.getMessage());
            return null;
        }
    }
} 