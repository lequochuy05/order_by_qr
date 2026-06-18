package com.qros.modules.menu.service;

import com.qros.modules.menu.dto.request.ItemOptionRequest;
import com.qros.modules.menu.dto.request.ItemOptionValueRequest;
import com.qros.modules.menu.model.ItemOption;
import com.qros.modules.menu.model.ItemOptionValue;
import com.qros.modules.menu.model.MenuItem;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MenuItemOptionService {

    public ItemOption buildItemOption(ItemOptionRequest req, MenuItem item) {
        ItemOption option = ItemOption.builder()
                .name(normalizeRequired(req.name(), "Option name cannot be empty"))
                .required(req.required() != null ? req.required() : false)
                .maxSelection(req.maxSelection() != null ? req.maxSelection() : 1)
                .displayOrder(0)
                .menuItem(item)
                .build();

        if (req.optionValues() != null) {
            for (int index = 0; index < req.optionValues().size(); index++) {
                ItemOptionValue value = buildItemOptionValue(req.optionValues().get(index), option);
                value.setDisplayOrder(index);
                option.getOptionValues().add(value);
            }
        }

        return option;
    }

    ItemOptionValue buildItemOptionValue(ItemOptionValueRequest req, ItemOption option) {
        return ItemOptionValue.builder()
                .name(normalizeRequired(req.name(), "Option value name cannot be empty"))
                .extraPrice(req.extraPrice() != null ? req.extraPrice() : BigDecimal.ZERO)
                .displayOrder(0)
                .itemOption(option)
                .build();
    }

    public void syncOptions(MenuItem item, List<ItemOptionRequest> incomingOptions) {
        if (incomingOptions == null || incomingOptions.isEmpty()) {
            item.getItemOptions().clear();
            return;
        }

        Map<Long, ItemOption> existingOptionsById = new HashMap<>();
        item.getItemOptions().stream()
                .filter(option -> option.getId() != null)
                .forEach(option -> existingOptionsById.put(option.getId(), option));

        Set<Long> incomingOptionIds = new HashSet<>();
        incomingOptions.stream()
                .map(ItemOptionRequest::id)
                .filter(optionId -> optionId != null)
                .forEach(incomingOptionIds::add);

        item.getItemOptions().removeIf(option -> option.getId() != null && !incomingOptionIds.contains(option.getId()));

        for (int index = 0; index < incomingOptions.size(); index++) {
            ItemOptionRequest optionReq = incomingOptions.get(index);
            ItemOption option = optionReq.id() == null ? null : existingOptionsById.get(optionReq.id());

            if (optionReq.id() != null && option == null) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Invalid item option id: " + optionReq.id());
            }

            if (option == null) {
                option = buildItemOption(optionReq, item);
                option.setDisplayOrder(index);
                item.getItemOptions().add(option);
            } else {
                option.setName(normalizeRequired(optionReq.name(), "Option name cannot be empty"));
                option.setRequired(optionReq.required() != null ? optionReq.required() : false);
                option.setMaxSelection(optionReq.maxSelection() != null ? optionReq.maxSelection() : 1);
                option.setDisplayOrder(index);
                syncOptionValues(option, optionReq.optionValues());
            }
        }
    }

    void syncOptionValues(ItemOption option, List<ItemOptionValueRequest> incomingValues) {
        if (incomingValues == null || incomingValues.isEmpty()) {
            option.getOptionValues().clear();
            return;
        }

        Map<Long, ItemOptionValue> existingValuesById = new HashMap<>();
        option.getOptionValues().stream()
                .filter(value -> value.getId() != null)
                .forEach(value -> existingValuesById.put(value.getId(), value));

        Set<Long> incomingValueIds = new HashSet<>();
        incomingValues.stream()
                .map(ItemOptionValueRequest::id)
                .filter(valueId -> valueId != null)
                .forEach(incomingValueIds::add);

        option.getOptionValues().removeIf(value -> value.getId() != null && !incomingValueIds.contains(value.getId()));

        for (int index = 0; index < incomingValues.size(); index++) {
            ItemOptionValueRequest valueReq = incomingValues.get(index);
            ItemOptionValue value = valueReq.id() == null ? null : existingValuesById.get(valueReq.id());

            if (valueReq.id() != null && value == null) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Invalid item option value id: " + valueReq.id());
            }

            if (value == null) {
                value = buildItemOptionValue(valueReq, option);
                value.setDisplayOrder(index);
                option.getOptionValues().add(value);
            } else {
                value.setName(normalizeRequired(valueReq.name(), "Option value name cannot be empty"));
                value.setExtraPrice(valueReq.extraPrice() != null ? valueReq.extraPrice() : BigDecimal.ZERO);
                value.setDisplayOrder(index);
            }
        }
    }

    public void validateOptions(List<ItemOptionRequest> options) {
        if (options == null || options.isEmpty()) {
            return;
        }

        Set<String> optionNames = new HashSet<>();

        for (ItemOptionRequest option : options) {
            String optionName = normalizeRequired(option.name(), "Option name cannot be empty")
                    .toLowerCase();

            if (!optionNames.add(optionName)) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Duplicate option name: " + option.name());
            }

            if (option.optionValues() == null || option.optionValues().isEmpty()) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Option must contain at least one value");
            }

            if (option.maxSelection() != null
                    && option.maxSelection() > option.optionValues().size()) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Max selection cannot exceed option value count");
            }

            Set<String> valueNames = new HashSet<>();

            for (ItemOptionValueRequest value : option.optionValues()) {
                String valueName = normalizeRequired(value.name(), "Option value name cannot be empty")
                        .toLowerCase();

                if (!valueNames.add(valueName)) {
                    throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Duplicate option value: " + value.name());
                }
            }
        }
    }

    static String normalizeRequired(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, message);
        }

        return value.trim();
    }
}
