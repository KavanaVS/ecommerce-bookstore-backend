package com.bookstore.dto;

import com.bookstore.model.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckoutRequest {

    @NotNull(message = "Shipping address is required")
    @Valid
    private AddressRequest shippingAddress;

    @NotNull(message = "Billing address is required")
    @Valid
    private AddressRequest billingAddress;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;
}
