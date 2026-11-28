package com.qros.modules.table.service;

import com.qros.modules.table.model.enums.TableStatus;
import com.qros.modules.table.repository.DiningTableRepository;
import com.qros.shared.subscription.SubscriptionResourceUsagePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TableSubscriptionUsageAdapter implements SubscriptionResourceUsagePort {

    private final DiningTableRepository diningTableRepository;

    @Override
    public ResourceType getResourceType() {
        return ResourceType.ACTIVE_TABLES;
    }

    @Override
    public long countActive() {
        return diningTableRepository.countByStatusNot(TableStatus.INACTIVE);
    }
}
