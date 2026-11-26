package com.qros.modules.menu.mapper;

import com.qros.modules.menu.model.ItemOption;
import com.qros.modules.menu.model.ItemOptionValue;
import java.util.Comparator;

final class MenuOptionComparators {

    static final Comparator<ItemOption> ITEM_OPTION_COMPARATOR = Comparator.comparing(
                    (ItemOption option) -> option.getDisplayOrder() == null ? 0 : option.getDisplayOrder())
            .thenComparing((ItemOption option) -> option.getName() == null ? "" : option.getName());

    static final Comparator<ItemOptionValue> ITEM_OPTION_VALUE_COMPARATOR = Comparator.comparing(
                    (ItemOptionValue value) -> value.getDisplayOrder() == null ? 0 : value.getDisplayOrder())
            .thenComparing((ItemOptionValue value) -> value.getName() == null ? "" : value.getName());

    private MenuOptionComparators() {}
}
