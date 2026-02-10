package com.dataloader.repository;

import com.dataloader.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByEmail(String email);
    Optional<Customer> findByCustomerCode(String customerCode);
    boolean existsByEmail(String email);
    boolean existsByCustomerCode(String customerCode);
}
