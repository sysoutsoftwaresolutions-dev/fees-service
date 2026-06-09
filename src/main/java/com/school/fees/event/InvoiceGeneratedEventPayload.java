package com.school.fees.event;

/**
 * Payload for the InvoiceGenerated event.
 */
public class InvoiceGeneratedEventPayload {
    private String invoiceId;
    private String studentId;
    private Double amount;
    private String dueDate;
    private String status;

    public InvoiceGeneratedEventPayload() {}

    public InvoiceGeneratedEventPayload(String invoiceId, String studentId, Double amount, String dueDate, String status) {
        this.invoiceId = invoiceId;
        this.studentId = studentId;
        this.amount = amount;
        this.dueDate = dueDate;
        this.status = status;
    }

    public String getInvoiceId() { return invoiceId; }
    public void setInvoiceId(String invoiceId) { this.invoiceId = invoiceId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
