package com.school.fees.event;

/**
 * Payload for the FeePaid event.
 */
public class FeePaidEventPayload {
    private String invoiceId;
    private String studentId;
    private Double amountPaid;
    private Long paymentTimestamp;

    public FeePaidEventPayload() {}

    public FeePaidEventPayload(String invoiceId, String studentId, Double amountPaid, Long paymentTimestamp) {
        this.invoiceId = invoiceId;
        this.studentId = studentId;
        this.amountPaid = amountPaid;
        this.paymentTimestamp = paymentTimestamp;
    }

    public String getInvoiceId() { return invoiceId; }
    public void setInvoiceId(String invoiceId) { this.invoiceId = invoiceId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public Double getAmountPaid() { return amountPaid; }
    public void setAmountPaid(Double amountPaid) { this.amountPaid = amountPaid; }

    public Long getPaymentTimestamp() { return paymentTimestamp; }
    public void setPaymentTimestamp(Long paymentTimestamp) { this.paymentTimestamp = paymentTimestamp; }
}
