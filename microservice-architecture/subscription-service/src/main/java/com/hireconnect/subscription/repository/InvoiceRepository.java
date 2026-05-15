package com.hireconnect.subscription.repository;

import com.hireconnect.subscription.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Integer> {

    List<Invoice> findBySubscriptionId(int subscriptionId);

    List<Invoice> findByRecruiterIdOrderByPaymentDateDesc(int recruiterId);

    Optional<Invoice> findTopBySubscriptionIdOrderByPaymentDateDesc(int subscriptionId);

    Optional<Invoice> findByTransactionId(String transactionId);
}