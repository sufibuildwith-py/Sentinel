package com.sentinel.revenue.execution;

import java.util.Optional;

public interface RazorpayGateway {
    PaymentLinkResource createPaymentLink(PaymentLinkCommand command);
    Optional<PaymentLinkResource> findPaymentLinkByReference(String referenceId);
    PaymentLinkResource fetchPaymentLink(String paymentLinkId);
    PaymentLinkResource cancelPaymentLink(String paymentLinkId);
    void resendNotification(String paymentLinkId, String medium);
    ProviderPayment fetchPayment(String paymentId);
}
