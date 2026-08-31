package com.sentinel.revenue.repository;
import com.sentinel.revenue.communication.PromiseStatus;
import com.sentinel.revenue.model.PromiseToPay;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface PromiseToPayRepository extends JpaRepository<PromiseToPay, UUID> {
    List<PromiseToPay> findAllByCustomerRefAndStatusIn(String customerRef, Collection<PromiseStatus> statuses);
    List<PromiseToPay> findAllByRecoveryActionIdAndStatusIn(UUID actionId, Collection<PromiseStatus> statuses);
    List<PromiseToPay> findAllByCustomerRef(String customerRef);
}
