package com.qros.modules.subscription.repository;

import com.qros.modules.subscription.model.RestaurantSubscription;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RestaurantSubscriptionRepository extends JpaRepository<RestaurantSubscription, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM RestaurantSubscription s WHERE s.id = :id")
    Optional<RestaurantSubscription> findByIdForUpdate(@Param("id") Long id);
}
