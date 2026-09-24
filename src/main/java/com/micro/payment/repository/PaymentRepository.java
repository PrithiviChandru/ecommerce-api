package com.micro.payment.repository;

import com.micro.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    boolean existsByOrderId(Long orderId);

    Page<Payment> findByOrderUserId(Long userId, Pageable pageable);

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);
}
