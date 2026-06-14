package com.smartmail.mail.dto;

import java.util.List;

public record MailSendResponse(Long mailId, String messageNo, MailDelivery delivery) {

    public MailSendResponse(Long mailId, String messageNo, List<String> delivered, List<String> failed) {
        this(mailId, messageNo, new MailDelivery(delivered, failed));
    }

    public record MailDelivery(List<String> delivered, List<String> failed) {}
}
