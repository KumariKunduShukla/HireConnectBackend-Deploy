package com.hireconnect.subscription.controller;

import com.hireconnect.subscription.dto.RazorpayOrderRequest;
import com.hireconnect.subscription.dto.RazorpayVerifyRequest;
import com.hireconnect.subscription.entity.Invoice;
import com.hireconnect.subscription.entity.Subscription;
import com.hireconnect.subscription.service.SubscriptionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionResourceTest {

    @Mock
    private SubscriptionService subscriptionService;

    @InjectMocks
    private SubscriptionResource subscriptionResource;

    @Test
    void listPlans() {
        when(subscriptionService.listPlans()).thenReturn(List.of());
        ResponseEntity<?> response = subscriptionResource.listPlans();
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void createOrder() {
        RazorpayOrderRequest req = new RazorpayOrderRequest();
        when(subscriptionService.createOrder(req)).thenReturn(Map.of("id", "order_123"));
        ResponseEntity<?> response = subscriptionResource.createOrder(req);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void verifyPayment() {
        RazorpayVerifyRequest req = new RazorpayVerifyRequest();
        when(subscriptionService.verifyPayment(req)).thenReturn(new Invoice());
        ResponseEntity<?> response = subscriptionResource.verifyPayment(req);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void cancel() {
        doNothing().when(subscriptionService).cancelSubscription(1);
        ResponseEntity<String> response = subscriptionResource.cancel(1);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void renew() {
        when(subscriptionService.renewSubscription(1)).thenReturn(Map.of("status", "renewed"));
        ResponseEntity<?> response = subscriptionResource.renew(1);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void getInvoices() {
        when(subscriptionService.getInvoices(1)).thenReturn(List.of());
        ResponseEntity<List<Invoice>> response = subscriptionResource.getInvoices(1);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void getInvoicesByRecruiter() {
        when(subscriptionService.getInvoicesByRecruiter(1)).thenReturn(List.of());
        ResponseEntity<List<Invoice>> response = subscriptionResource.getInvoicesByRecruiter(1);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void getSubscriptionsByRecruiter() {
        when(subscriptionService.getSubscriptionsByRecruiter(1)).thenReturn(List.of());
        ResponseEntity<List<Subscription>> response = subscriptionResource.getSubscriptionsByRecruiter(1);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void getAllSubscriptions() {
        when(subscriptionService.getAllSubscriptions()).thenReturn(List.of());
        ResponseEntity<List<Subscription>> response = subscriptionResource.getAllSubscriptions();
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void downloadInvoicePdf_Error() {
        when(subscriptionService.getInvoicePdf(1, 2)).thenThrow(new RuntimeException("Error"));
        assertThrows(ResponseStatusException.class, () -> subscriptionResource.downloadInvoicePdf(1, 2));
    }

    @Test
    void createOrder_Error() {
        when(subscriptionService.createOrder(any())).thenThrow(new RuntimeException("Error"));
        ResponseEntity<?> response = subscriptionResource.createOrder(new RazorpayOrderRequest());
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void verifyPayment_Error() {
        when(subscriptionService.verifyPayment(any())).thenThrow(new RuntimeException("Error"));
        ResponseEntity<?> response = subscriptionResource.verifyPayment(new RazorpayVerifyRequest());
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void cancel_Error() {
        doThrow(new RuntimeException("Error")).when(subscriptionService).cancelSubscription(1);
        ResponseEntity<String> response = subscriptionResource.cancel(1);
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void renew_Error() {
        when(subscriptionService.renewSubscription(1)).thenThrow(new RuntimeException("Error"));
        ResponseEntity<?> response = subscriptionResource.renew(1);
        assertEquals(400, response.getStatusCode().value());
    }
}
