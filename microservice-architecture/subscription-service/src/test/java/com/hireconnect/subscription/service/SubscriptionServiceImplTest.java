package com.hireconnect.subscription.service;

import com.hireconnect.subscription.dto.RazorpayOrderRequest;
import com.hireconnect.subscription.dto.RazorpayVerifyRequest;
import com.hireconnect.subscription.entity.Invoice;
import com.hireconnect.subscription.entity.Subscription;
import com.hireconnect.subscription.repository.InvoiceRepository;
import com.hireconnect.subscription.repository.SubscriptionRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class SubscriptionServiceImplTest {

    @Mock
    private SubscriptionRepository subRepo;

    @Mock
    private InvoiceRepository invoiceRepo;

    @Mock
    private RazorpayClient razorpayClient;

    @Mock
    private InvoiceEmailService invoiceEmailService;

    @Mock
    private InvoicePdfService invoicePdfService;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private SubscriptionServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "razorpayKeyId", "test_key");
        ReflectionTestUtils.setField(service, "razorpayKeySecret", "test_secret");
        
        // Mock Razorpay objects
        razorpayClient.orders = mock(com.razorpay.OrderClient.class);
    }

    @Test
    void listPlans() {
        assertEquals(3, service.listPlans().size());
    }

    @Test
    void createOrder_FreePlan() {
        RazorpayOrderRequest request = new RazorpayOrderRequest();
        request.setRecruiterId(1);
        request.setPlan("FREE");
        request.setBillingEmail("test@test.com");

        when(subRepo.save(any(Subscription.class))).thenAnswer(i -> {
            Subscription s = i.getArgument(0);
            s.setSubscriptionId(100);
            return s;
        });

        Map<String, Object> result = service.createOrder(request);
        assertEquals("FREE", result.get("plan"));
        verify(subRepo, times(1)).save(any(Subscription.class));
    }

    @Test
    void createOrder_PaidPlan_Success() throws RazorpayException {
        RazorpayOrderRequest request = new RazorpayOrderRequest();
        request.setRecruiterId(1);
        request.setPlan("PROFESSIONAL");

        Order mockOrder = mock(Order.class);
        when(mockOrder.get("id")).thenReturn("order_123");
        when(razorpayClient.orders.create(any(JSONObject.class))).thenReturn(mockOrder);

        when(subRepo.findByRecruiterId(1)).thenReturn(new ArrayList<>());
        when(subRepo.save(any(Subscription.class))).thenReturn(new Subscription());

        Map<String, Object> result = service.createOrder(request);
        assertEquals("order_123", result.get("orderId"));
    }

    @Test
    void createOrder_AlreadyHasActivePaidPlan() {
        RazorpayOrderRequest request = new RazorpayOrderRequest();
        request.setRecruiterId(1);
        request.setPlan("PROFESSIONAL");

        Subscription active = new Subscription();
        active.setStatus("ACTIVE");
        active.setPlan("ENTERPRISE");
        when(subRepo.findByRecruiterId(1)).thenReturn(List.of(active));

        assertThrows(RuntimeException.class, () -> service.createOrder(request));
    }

    @Test
    void verifyPayment_Success() {
        RazorpayVerifyRequest request = new RazorpayVerifyRequest();
        request.setRazorpayOrderId("order_123");
        request.setRazorpayPaymentId("pay_123");
        request.setRazorpaySignature("MOCK_SIGNATURE");

        Subscription sub = new Subscription();
        sub.setSubscriptionId(1);
        sub.setRecruiterId(1);
        sub.setPlan("PROFESSIONAL");
        sub.setStatus("PENDING");
        
        when(subRepo.findByRazorpayOrderId("order_123")).thenReturn(Optional.of(sub));
        when(invoiceRepo.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        Invoice inv = service.verifyPayment(request);
        assertEquals("ACTIVE", sub.getStatus());
        assertEquals("PAID", inv.getStatus());
    }

    @Test
    void verifyPayment_Renewal() {
        RazorpayVerifyRequest request = new RazorpayVerifyRequest();
        request.setRazorpayOrderId("order_123");
        request.setRazorpayPaymentId("pay_123");
        request.setRazorpaySignature("MOCK_SIGNATURE");

        Subscription sub = new Subscription();
        sub.setSubscriptionId(1);
        sub.setStatus("ACTIVE");
        sub.setEndDate(LocalDateTime.now().plusDays(10));
        
        when(subRepo.findByRazorpayOrderId("order_123")).thenReturn(Optional.of(sub));
        when(invoiceRepo.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        service.verifyPayment(request);
        // Should extend by 1 year from current end date
        assertTrue(sub.getEndDate().isAfter(LocalDateTime.now().plusYears(1)));
    }

    @Test
    void cancelSubscription() {
        Subscription sub = new Subscription();
        sub.setStatus("ACTIVE");
        when(subRepo.findById(1)).thenReturn(Optional.of(sub));
        
        service.cancelSubscription(1);
        assertEquals("CANCELLED", sub.getStatus());
    }

    @Test
    void renewSubscription_Success() throws RazorpayException {
        Subscription sub = new Subscription();
        sub.setSubscriptionId(1);
        sub.setRecruiterId(1);
        sub.setPlan("PROFESSIONAL");
        sub.setStatus("ACTIVE");
        
        when(subRepo.findById(1)).thenReturn(Optional.of(sub));
        
        Order mockOrder = mock(Order.class);
        when(mockOrder.get("id")).thenReturn("order_renew_123");
        when(razorpayClient.orders.create(any(JSONObject.class))).thenReturn(mockOrder);

        Map<String, Object> result = service.renewSubscription(1);
        assertEquals("order_renew_123", result.get("orderId"));
    }

    @Test
    void renewSubscription_LongReceipt() throws RazorpayException {
        Subscription sub = new Subscription();
        sub.setSubscriptionId(1);
        sub.setRecruiterId(1234567890); // Long ID to help make receipt > 40
        sub.setPlan("PROFESSIONAL");
        sub.setStatus("ACTIVE");
        
        when(subRepo.findById(1)).thenReturn(Optional.of(sub));
        
        Order mockOrder = mock(Order.class);
        when(mockOrder.get("id")).thenReturn("order_renew_long");
        when(razorpayClient.orders.create(any(JSONObject.class))).thenReturn(mockOrder);

        service.renewSubscription(1);
        // This should trigger line 340 branch
    }

    @Test
    void getInvoicePdf() {
        Invoice inv = new Invoice();
        inv.setRecruiterId(1);
        inv.setSubscriptionId(10);
        when(invoiceRepo.findById(100)).thenReturn(Optional.of(inv));
        when(subRepo.findById(10)).thenReturn(Optional.of(new Subscription()));
        when(invoicePdfService.generatePdf(any(), any())).thenReturn(new byte[]{1, 2, 3});

        byte[] pdf = service.getInvoicePdf(100, 1);
        assertArrayEquals(new byte[]{1, 2, 3}, pdf);
    }


    @Test
    void getInvoicePdf_WrongRecruiter() {
        Invoice inv = new Invoice();
        inv.setRecruiterId(2);
        when(invoiceRepo.findById(100)).thenReturn(Optional.of(inv));

        assertThrows(RuntimeException.class, () -> service.getInvoicePdf(100, 1));
    }

    @Test
    void getAllSubscriptions() {
        service.getAllSubscriptions();
        verify(subRepo).findAll();
    }

    @Test
    void getInvoicesByRecruiter() {
        service.getInvoicesByRecruiter(1);
        verify(invoiceRepo).findByRecruiterIdOrderByPaymentDateDesc(1);
    }

    @Test
    void getInvoicesBySubscription() {
        service.getInvoices(10);
        verify(invoiceRepo).findBySubscriptionId(10);
    }

    @Test
    void createOrder_InvalidRecruiter() {
        RazorpayOrderRequest req = new RazorpayOrderRequest();
        req.setRecruiterId(0);
        assertThrows(RuntimeException.class, () -> service.createOrder(req));
    }

    @Test
    void createOrder_MissingPlan() {
        RazorpayOrderRequest req = new RazorpayOrderRequest();
        req.setRecruiterId(1);
        assertThrows(RuntimeException.class, () -> service.createOrder(req));
    }

    @Test
    void createOrder_RazorpayException() throws RazorpayException {
        RazorpayOrderRequest req = new RazorpayOrderRequest();
        req.setRecruiterId(1);
        req.setPlan("PROFESSIONAL");
        when(razorpayClient.orders.create(any())).thenThrow(new RazorpayException("Bad Request"));
        assertThrows(RuntimeException.class, () -> service.createOrder(req));
    }

    @Test
    void verifyPayment_InvalidSignature() {
        RazorpayVerifyRequest req = new RazorpayVerifyRequest();
        req.setRazorpayOrderId("o1");
        req.setRazorpayPaymentId("p1");
        req.setRazorpaySignature("wrong");
        assertThrows(RuntimeException.class, () -> service.verifyPayment(req));
    }

    @Test
    void renewSubscription_InvalidStatus() {
        Subscription sub = new Subscription();
        sub.setStatus("PENDING");
        when(subRepo.findById(1)).thenReturn(Optional.of(sub));
        assertThrows(RuntimeException.class, () -> service.renewSubscription(1));
    }

    @Test
    void renewSubscription_FreePlan() {
        Subscription sub = new Subscription();
        sub.setStatus("ACTIVE");
        sub.setPlan("FREE");
        when(subRepo.findById(1)).thenReturn(Optional.of(sub));
        assertThrows(RuntimeException.class, () -> service.renewSubscription(1));
    }

    @Test
    void sendNotification_Exception() {
        // Test the catch block in sendNotification
        Subscription sub = new Subscription();
        sub.setRecruiterId(1);
        sub.setPlan("PROFESSIONAL");
        sub.setBillingEmail("test@test.com");
        when(subRepo.findByRazorpayOrderId(anyString())).thenReturn(Optional.of(sub));
        doThrow(new RuntimeException("API Down")).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
        
        RazorpayVerifyRequest req = new RazorpayVerifyRequest();
        req.setRazorpayOrderId("o1");
        req.setRazorpaySignature("MOCK_SIGNATURE");
        
        assertDoesNotThrow(() -> service.verifyPayment(req));
    }

    @Test
    void createOrder_RazorpayException_WithJson() throws RazorpayException {
        RazorpayOrderRequest req = new RazorpayOrderRequest();
        req.setRecruiterId(1);
        req.setPlan("PROFESSIONAL");
        
        RazorpayException ex = mock(RazorpayException.class);
        when(ex.getMessage()).thenReturn("{\"error\":{\"description\":\"Plan expired\"}}");
        
        when(razorpayClient.orders.create(any())).thenThrow(ex);
        
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> service.createOrder(req));
        assertTrue(thrown.getMessage().contains("Plan expired"));
    }

    @Test
    void describeRazorpayFailure_Null() throws RazorpayException {
        // This is a private method, but we can trigger it via createOrder(null) if we mock it?
        // Actually I'll just use ReflectionTestUtils to test the private static method if needed,
        // but better to trigger via public API.
        
        RazorpayOrderRequest req = new RazorpayOrderRequest();
        req.setRecruiterId(1);
        req.setPlan("PROFESSIONAL");
        
        // Throw a RazorpayException with null message
        when(razorpayClient.orders.create(any())).thenThrow(new RazorpayException((String) null));
        
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> service.createOrder(req));
        assertTrue(thrown.getMessage().contains("RazorpayException"));
    }

    @Test
    void verifyPayment_NoEmailInRequest() {
        RazorpayVerifyRequest req = new RazorpayVerifyRequest();
        req.setRazorpayOrderId("o1");
        req.setRazorpaySignature("MOCK_SIGNATURE");
        req.setBillingEmail(null); // Should skip notification if sub email also null
        
        Subscription sub = new Subscription();
        sub.setBillingEmail(null);
        when(subRepo.findByRazorpayOrderId(anyString())).thenReturn(Optional.of(sub));
        when(invoiceRepo.save(any())).thenReturn(new Invoice());

        assertDoesNotThrow(() -> service.verifyPayment(req));
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void testPrivateMethods() {
        // Test firstNonBlank
        String res = (String) ReflectionTestUtils.invokeMethod(service, "firstNonBlank", "a", "b");
        assertEquals("a", res);
        res = (String) ReflectionTestUtils.invokeMethod(service, "firstNonBlank", null, "b");
        assertEquals("b", res);
        res = (String) ReflectionTestUtils.invokeMethod(service, "firstNonBlank", "", "b");
        assertEquals("b", res);
        res = (String) ReflectionTestUtils.invokeMethod(service, "firstNonBlank", null, null);
        assertNull(res);
    }

    @Test
    void testCancelOtherPendingSubscriptions() {
        Subscription s1 = new Subscription();
        s1.setSubscriptionId(1);
        s1.setStatus("PENDING");
        Subscription s2 = new Subscription();
        s2.setSubscriptionId(2);
        s2.setStatus("ACTIVE");
        
        when(subRepo.findByRecruiterId(1)).thenReturn(List.of(s1, s2));
        
        ReflectionTestUtils.invokeMethod(service, "cancelOtherPendingSubscriptions", 1, 99);
        assertEquals("CANCELLED", s1.getStatus());
        assertEquals("ACTIVE", s2.getStatus());
    }

    @Test
    void testCancelActiveFreePlansExcept() {
        Subscription s1 = new Subscription();
        s1.setSubscriptionId(1);
        s1.setStatus("ACTIVE");
        s1.setPlan("FREE");
        Subscription s2 = new Subscription();
        s2.setSubscriptionId(2);
        s2.setStatus("ACTIVE");
        s2.setPlan("PROFESSIONAL");
        
        when(subRepo.findByRecruiterId(1)).thenReturn(List.of(s1, s2));
        
        ReflectionTestUtils.invokeMethod(service, "cancelActiveFreePlansExcept", 1, 99);
        assertEquals("CANCELLED", s1.getStatus());
        assertEquals("ACTIVE", s2.getStatus());
    }

    @Test
    void describeRazorpayFailure_RawJsonNoDescription() {
        RazorpayException ex = mock(RazorpayException.class);
        when(ex.getMessage()).thenReturn("{\"error\":{}}");
        String res = (String) ReflectionTestUtils.invokeMethod(service, "describeRazorpayFailure", ex);
        assertEquals("{\"error\":{}}", res);
    }
    @Test
    void testPrivateMethods_Blank() {
        String res = (String) ReflectionTestUtils.invokeMethod(service, "firstNonBlank", "  ", "b");
        assertEquals("b", res);
    }

    @Test
    void verifySignature_Failure() {
        RazorpayVerifyRequest req = new RazorpayVerifyRequest();
        req.setRazorpayOrderId("o1");
        req.setRazorpayPaymentId("p1");
        req.setRazorpaySignature("invalid");
        
        Subscription sub = new Subscription();
        when(subRepo.findByRazorpayOrderId(anyString())).thenReturn(Optional.of(sub));
        assertThrows(RuntimeException.class, () -> service.verifyPayment(req));
    }

    @Test
    void verifyPayment_SubscriptionNotFound() {
        RazorpayVerifyRequest req = new RazorpayVerifyRequest();
        req.setRazorpayOrderId("o1");
        req.setRazorpaySignature("MOCK_SIGNATURE");
        when(subRepo.findByRazorpayOrderId("o1")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.verifyPayment(req));
    }

    @Test
    void renewSubscription_NotFound() {
        when(subRepo.findById(1)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.renewSubscription(1));
    }

    @Test
    void renewSubscription_RazorpayFailure() throws RazorpayException {
        Subscription sub = new Subscription();
        sub.setStatus("ACTIVE");
        sub.setPlan("PROFESSIONAL");
        when(subRepo.findById(1)).thenReturn(Optional.of(sub));
        when(razorpayClient.orders.create(any())).thenThrow(new RazorpayException("Failed"));
        assertThrows(RuntimeException.class, () -> service.renewSubscription(1));
    }

    @Test
    void getInvoicePdf_NotFound() {
        when(invoiceRepo.findById(1)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.getInvoicePdf(1, 1));
    }

    @Test
    void describeRazorpayFailure_NonJson() {
        RazorpayException ex = mock(RazorpayException.class);
        when(ex.getMessage()).thenReturn("Plain text error");
        String res = (String) ReflectionTestUtils.invokeMethod(service, "describeRazorpayFailure", ex);
        assertEquals("Plain text error", res);
    }

    @Test
    void describeRazorpayFailure_NullEx() {
        String res = (String) ReflectionTestUtils.invokeMethod(service, "describeRazorpayFailure", (Object) null);
        assertEquals("unknown error", res);
    }

    @Test
    void describeRazorpayFailure_EmptyMessage() {
        RazorpayException ex = mock(RazorpayException.class);
        when(ex.getMessage()).thenReturn("");
        String res = (String) ReflectionTestUtils.invokeMethod(service, "describeRazorpayFailure", ex);
        assertEquals("RazorpayException", res);
    }

    @Test
    void testGenerateInvoice_CatchBlock() {
        Subscription sub = new Subscription();
        sub.setBillingEmail("test@test.com");
        
        Invoice mockInvoice = new Invoice();
        mockInvoice.setInvoiceId(123);
        when(invoiceRepo.save(any())).thenReturn(mockInvoice);
        
        doThrow(new RuntimeException("Email Failed")).when(invoiceEmailService).sendInvoiceEmail(any(), any(), anyString());
        
        Invoice inv = service.generateInvoice(sub, "MODE", "TXN");
        assertNotNull(inv);
        verify(invoiceEmailService).sendInvoiceEmail(any(), any(), eq("test@test.com"));
    }

    @Test
    void getSubscriptionsByRecruiter() {
        when(subRepo.findByRecruiterId(1)).thenReturn(List.of(new Subscription()));
        List<Subscription> result = service.getSubscriptionsByRecruiter(1);
        assertEquals(1, result.size());
        verify(subRepo).findByRecruiterId(1);
    }

    @Test
    void sendNotification_NullEmail() {
        // Trigger line 496: return if email is null
        ReflectionTestUtils.invokeMethod(service, "sendNotification", 1, null, "TYPE", "MSG");
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void sendNotification_Success() {
        ReflectionTestUtils.invokeMethod(service, "sendNotification", 1, "test@test.com", "TYPE", "MSG");
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void sendNotification_Failure() {
        doThrow(new RuntimeException("Error")).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
        ReflectionTestUtils.invokeMethod(service, "sendNotification", 1, "test@test.com", "TYPE", "MSG");
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
    }
}
