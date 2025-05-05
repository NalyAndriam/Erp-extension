package com.eval.erp.service;

import com.eval.erp.model.PurchaseInvoiceDTO;
import com.eval.erp.model.PaymentEntryDTO;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PurchaseInvoiceService {

    private static final Logger logger = LoggerFactory.getLogger(PurchaseInvoiceService.class);

    @Value("${erpnext.default_payment_account}")
    private String defaultPaymentAccount;

    @Value("${erpnext.default_payment_account_currency}")
    private String defaultPaymentAccountCurrency;

    private final ErpNextApiService erpNextApiService;

    public PurchaseInvoiceService(ErpNextApiService erpNextApiService) {
        this.erpNextApiService = erpNextApiService;
    }

    private String getSid(HttpSession session) {
        String sid = (String) session.getAttribute("erp_sid");
        if (sid == null) {
            throw new IllegalStateException("ERPNext session not found. Please reconnect.");
        }
        return sid;
    }

    public List<PurchaseInvoiceDTO> getPurchaseInvoices(HttpSession session) throws Exception {
        String sid = getSid(session);
        try {
            String fields = "[\"*\"]";
            ResponseEntity<Map> response = erpNextApiService.getResource("Purchase Invoice", fields, null, sid);
            List<Map<String, Object>> invoiceData = (List<Map<String, Object>>) response.getBody().get("data");

            List<PurchaseInvoiceDTO> invoices = new ArrayList<>();
            for (Map<String, Object> data : invoiceData) {
                PurchaseInvoiceDTO dto = new PurchaseInvoiceDTO();
                dto.setName((String) data.get("name"));
                dto.setSupplier((String) data.get("supplier"));
                dto.setStatus((String) data.get("status"));
                dto.setGrandTotal(data.get("grand_total") instanceof Number ? ((Number) data.get("grand_total")).doubleValue() : null);
                dto.setOutstandingAmount(data.get("outstanding_amount") instanceof Number ? ((Number) data.get("outstanding_amount")).doubleValue() : null);
                dto.setPostingDate((String) data.get("posting_date"));
                dto.setDueDate((String) data.get("due_date"));
                // Items not needed for listing, left null
                dto.setAdditionalFields(new HashMap<>(data));
                invoices.add(dto);
            }
            return invoices;
        } catch (Exception e) {
            logger.error("Error fetching invoices: {}", e.getMessage());
            throw new Exception("Error fetching invoices: " + e.getMessage());
        }
    }

    public PurchaseInvoiceDTO getInvoiceDetails(String id, HttpSession session) throws Exception {
        String sid = getSid(session);
        try {
            ResponseEntity<Map> response = erpNextApiService.getResourceById("Purchase Invoice", id, null, sid);
            Map<String, Object> invoiceData = (Map<String, Object>) response.getBody().get("data");

            if (invoiceData == null) {
                throw new Exception("Invoice not found.");
            }

            PurchaseInvoiceDTO dto = new PurchaseInvoiceDTO();
            dto.setName((String) invoiceData.get("name"));
            dto.setSupplier((String) invoiceData.get("supplier"));
            dto.setStatus((String) invoiceData.get("status"));
            dto.setGrandTotal(invoiceData.get("grand_total") instanceof Number ? ((Number) invoiceData.get("grand_total")).doubleValue() : null);
            dto.setOutstandingAmount(invoiceData.get("outstanding_amount") instanceof Number ? ((Number) invoiceData.get("outstanding_amount")).doubleValue() : null);
            dto.setPostingDate((String) invoiceData.get("posting_date"));
            dto.setDueDate((String) invoiceData.get("due_date"));
            dto.setItems((List<Map<String, Object>>) invoiceData.get("items"));
            dto.setAdditionalFields(new HashMap<>(invoiceData));

            return dto;
        } catch (Exception e) {
            logger.error("Error fetching invoice details: {}", e.getMessage());
            throw new Exception("Error fetching invoice details: " + e.getMessage());
        }
    }

    public Map<String, Object> getPaymentFormData(String id, HttpSession session) throws Exception {
        String sid = getSid(session);
        try {
            // Fetch invoice details
            String invoiceFields = "[\"name\",\"supplier\",\"grand_total\",\"outstanding_amount\",\"due_date\",\"status\"]";
            ResponseEntity<Map> invoiceResponse = erpNextApiService.getResourceById("Purchase Invoice", id, invoiceFields, sid);
            Map<String, Object> invoiceData = (Map<String, Object>) invoiceResponse.getBody().get("data");

            if (invoiceData == null || (!"Unpaid".equals(invoiceData.get("status")) &&
                    !"Partially Paid".equals(invoiceData.get("status")) && !"Overdue".equals(invoiceData.get("status")))) {
                throw new Exception("Invoice not found or not in Unpaid/Partially Paid/Overdue status.");
            }

            // Fetch modes of payment
            String modeFields = "[\"name\"]";
            ResponseEntity<Map> modesResponse = erpNextApiService.getResource("Mode of Payment", modeFields, null, sid);
            List<Map<String, Object>> modesOfPayment = (List<Map<String, Object>>) modesResponse.getBody().get("data");

            // Prepare data for the view
            Map<String, Object> formData = new HashMap<>();
            PurchaseInvoiceDTO invoiceDTO = new PurchaseInvoiceDTO();
            invoiceDTO.setName((String) invoiceData.get("name"));
            invoiceDTO.setSupplier((String) invoiceData.get("supplier"));
            invoiceDTO.setStatus((String) invoiceData.get("status"));
            invoiceDTO.setGrandTotal(invoiceData.get("grand_total") instanceof Number ? ((Number) invoiceData.get("grand_total")).doubleValue() : null);
            invoiceDTO.setOutstandingAmount(invoiceData.get("outstanding_amount") instanceof Number ? ((Number) invoiceData.get("outstanding_amount")).doubleValue() : null);
            invoiceDTO.setDueDate((String) invoiceData.get("due_date"));
            formData.put("invoice", invoiceDTO);
            formData.put("modesOfPayment", modesOfPayment);

            return formData;
        } catch (Exception e) {
            logger.error("Error fetching payment form data: {}", e.getMessage());
            throw new Exception("Error fetching payment form data: " + e.getMessage());
        }
    }

    public void createPayment(PaymentEntryDTO paymentEntry, HttpSession session) throws Exception {
        String sid = getSid(session);

        // Validate required fields
        if (paymentEntry.getModeOfPayment() == null || paymentEntry.getModeOfPayment().trim().isEmpty()) {
            throw new Exception("Mode of Payment is required.");
        }
        if (paymentEntry.getSourceExchangeRate() == null || paymentEntry.getSourceExchangeRate() <= 0) {
            paymentEntry.setSourceExchangeRate(1.0);
        }
        if (paymentEntry.getPaidAmount() == null || paymentEntry.getPaidAmount() <= 0) {
            throw new Exception("Paid Amount must be greater than 0.");
        }
        if (paymentEntry.getReceivedAmount() != null && Math.abs(paymentEntry.getReceivedAmount() - paymentEntry.getPaidAmount()) > 0.01) {
            throw new Exception("Received Amount must equal Paid Amount.");
        }

        try {
            // Fetch invoice details
            String invoiceFields = "[\"name\",\"outstanding_amount\",\"company\",\"credit_to\",\"currency\",\"status\"]";
            ResponseEntity<Map> invoiceResponse = erpNextApiService.getResourceById("Purchase Invoice", paymentEntry.getInvoiceId(), invoiceFields, sid);
            Map<String, Object> invoiceData = (Map<String, Object>) invoiceResponse.getBody().get("data");

            if (invoiceData == null) {
                throw new Exception("Invoice not found.");
            }

            Double outstandingAmount = ((Number) invoiceData.get("outstanding_amount")).doubleValue();
            String company = (String) invoiceData.get("company");
            String creditTo = (String) invoiceData.get("credit_to");
            String invoiceCurrency = (String) invoiceData.get("currency");

            if (paymentEntry.getPaidAmount() > outstandingAmount) {
                throw new Exception("Paid Amount cannot exceed Outstanding Amount (" + outstandingAmount + ").");
            }
            if (creditTo == null || creditTo.trim().isEmpty()) {
                throw new Exception("No creditor account associated with the invoice.");
            }

            // Set allocated_amount to paid_amount
            paymentEntry.setAllocatedAmount(paymentEntry.getPaidAmount());

            // Prepare payment entry data
            Map<String, Object> paymentData = new HashMap<>();
            paymentData.put("doctype", "Payment Entry");
            paymentData.put("payment_type", paymentEntry.getPaymentType());
            paymentData.put("mode_of_payment", paymentEntry.getModeOfPayment());
            paymentData.put("party_type", paymentEntry.getPartyType());
            paymentData.put("party", paymentEntry.getParty());
            paymentData.put("source_exchange_rate", paymentEntry.getSourceExchangeRate());
            paymentData.put("paid_from", defaultPaymentAccount);
            paymentData.put("paid_from_account_currency", defaultPaymentAccountCurrency);
            paymentData.put("company", company);
            paymentData.put("paid_to", creditTo);
            paymentData.put("paid_to_account_currency", invoiceCurrency);
            paymentData.put("posting_date", paymentEntry.getPostingDate() != null && !paymentEntry.getPostingDate().trim().isEmpty()
                    ? paymentEntry.getPostingDate() : LocalDate.now().toString());
            if (paymentEntry.getReferenceNo() != null && !paymentEntry.getReferenceNo().trim().isEmpty()) {
                paymentData.put("reference_no", paymentEntry.getReferenceNo());
            }
            if (paymentEntry.getReferenceDate() != null && !paymentEntry.getReferenceDate().trim().isEmpty()) {
                paymentData.put("reference_date", paymentEntry.getReferenceDate());
            }
            paymentData.put("paid_amount", paymentEntry.getPaidAmount());
            paymentData.put("received_amount", paymentEntry.getPaidAmount());
            paymentData.put("base_paid_amount", paymentEntry.getPaidAmount());
            paymentData.put("base_received_amount", paymentEntry.getPaidAmount());

            // Add reference to the invoice
            List<Map<String, Object>> references = new ArrayList<>();
            Map<String, Object> reference = new HashMap<>();
            reference.put("reference_doctype", "Purchase Invoice");
            reference.put("reference_name", paymentEntry.getInvoiceId());
            reference.put("allocated_amount", paymentEntry.getAllocatedAmount());
            references.add(reference);
            paymentData.put("references", references);

            // Create Payment Entry
            ResponseEntity<Map> createResponse = erpNextApiService.createResource("Payment Entry", paymentData, sid);

            // Extract Payment Entry name
            Map<String, Object> createResponseData = (Map<String, Object>) createResponse.getBody().get("data");
            String paymentEntryName = (String) createResponseData.get("name");
            if (paymentEntryName == null || paymentEntryName.trim().isEmpty()) {
                throw new Exception("Failed to retrieve Payment Entry name after creation.");
            }

            // Fetch Payment Entry to avoid TimestampMismatchError
            ResponseEntity<Map> paymentEntryResponse = erpNextApiService.getResourceById("Payment Entry", paymentEntryName, null, sid);
            Map<String, Object> paymentEntryData = (Map<String, Object>) paymentEntryResponse.getBody().get("data");
            if (paymentEntryData == null) {
                throw new Exception("Failed to retrieve Payment Entry after creation.");
            }

            // Submit Payment Entry
            Map<String, Object> submitData = new HashMap<>();
            submitData.put("doc", paymentEntryData);
            ResponseEntity<Map> submitResponse = erpNextApiService.submitDocument(submitData, sid);

            logger.info("Payment Entry {} submitted successfully for invoice {}", paymentEntryName, paymentEntry.getInvoiceId());
        } catch (Exception e) {
            logger.error("Error creating payment: {}", e.getMessage());
            throw new Exception("Error creating payment: " + e.getMessage());
        }
    }
}