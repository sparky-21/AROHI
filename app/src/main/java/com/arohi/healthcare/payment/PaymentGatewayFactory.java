package com.arohi.healthcare.payment;

import com.arohi.healthcare.models.PaymentGatewayType;

import java.util.HashMap;
import java.util.Map;

/**
 * Phase 13 Payment Gateway Factory.
 * Instantiates and caches payment gateway adapters for runtime selection.
 */
public class PaymentGatewayFactory {

    private static final Map<PaymentGatewayType, PaymentGateway> GATEWAY_REGISTRY = new HashMap<>();

    static {
        GATEWAY_REGISTRY.put(PaymentGatewayType.BKASH, new BkashPaymentGateway());
        GATEWAY_REGISTRY.put(PaymentGatewayType.NAGAD, new NagadPaymentGateway());
        GATEWAY_REGISTRY.put(PaymentGatewayType.CARD_SSLCOMMERZ, new CardPaymentGateway());
        GATEWAY_REGISTRY.put(PaymentGatewayType.CASH_COUNTER, new CashPaymentGateway());
    }

    public static PaymentGateway getGateway(PaymentGatewayType type) {
        if (type == null) type = PaymentGatewayType.BKASH;
        PaymentGateway gateway = GATEWAY_REGISTRY.get(type);
        return gateway != null ? gateway : GATEWAY_REGISTRY.get(PaymentGatewayType.BKASH);
    }
}
