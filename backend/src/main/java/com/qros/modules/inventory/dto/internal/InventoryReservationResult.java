package com.qros.modules.inventory.dto.internal;

import java.util.List;

public record InventoryReservationResult(Long orderItemId, boolean success, List<InventoryRequirement> requirements) {}
