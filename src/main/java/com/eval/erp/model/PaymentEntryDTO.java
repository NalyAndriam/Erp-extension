package com.eval.erp.model;


public class PaymentEntryDTO {
    private String paymentType;
    private String modeOfPayment;
    private Double paidAmount;
    private Double receivedAmount;
    private String partyType;
    private String party;
    private String postingDate;
    private Double allocatedAmount;
    private String referenceNo;
    private String referenceDate;
    private Double sourceExchangeRate;
    private String invoiceId;

    // Constructor
    public PaymentEntryDTO() {}

    // Getters and Setters
    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }
    public String getModeOfPayment() { return modeOfPayment; }
    public void setModeOfPayment(String modeOfPayment) { this.modeOfPayment = modeOfPayment; }
    public Double getPaidAmount() { return paidAmount; }
    public void setPaidAmount(Double paidAmount) { this.paidAmount = paidAmount; }
    public Double getReceivedAmount() { return receivedAmount; }
    public void setReceivedAmount(Double receivedAmount) { this.receivedAmount = receivedAmount; }
    public String getPartyType() { return partyType; }
    public void setPartyType(String partyType) { this.partyType = partyType; }
    public String getParty() { return party; }
    public void setParty(String party) { this.party = party; }
    public String getPostingDate() { return postingDate; }
    public void setPostingDate(String postingDate) { this.postingDate = postingDate; }
    public Double getAllocatedAmount() { return allocatedAmount; }
    public void setAllocatedAmount(Double allocatedAmount) { this.allocatedAmount = allocatedAmount; }
    public String getReferenceNo() { return referenceNo; }
    public void setReferenceNo(String referenceNo) { this.referenceNo = referenceNo; }
    public String getReferenceDate() { return referenceDate; }
    public void setReferenceDate(String referenceDate) { this.referenceDate = referenceDate; }
    public Double getSourceExchangeRate() { return sourceExchangeRate; }
    public void setSourceExchangeRate(Double sourceExchangeRate) { this.sourceExchangeRate = sourceExchangeRate; }
    public String getInvoiceId() { return invoiceId; }
    public void setInvoiceId(String invoiceId) { this.invoiceId = invoiceId; }
}