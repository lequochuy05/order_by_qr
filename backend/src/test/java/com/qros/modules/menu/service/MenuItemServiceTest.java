package com.qros.modules.menu.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qros.infrastructure.storage.StorageService;
import com.qros.modules.inventory.service.InventoryAvailabilityService;
import com.qros.modules.menu.dto.response.MenuItemResponse;
import com.qros.modules.menu.mapper.MenuItemMapper;
import com.qros.modules.menu.mapper.PublicMenuMapper;
import com.qros.modules.menu.model.Category;
import com.qros.modules.menu.model.MenuItem;
import com.qros.modules.menu.repository.CategoryRepository;
import com.qros.modules.menu.repository.ComboItemRepository;
import com.qros.modules.menu.repository.MenuItemRepository;
import com.qros.shared.event.DomainEvents.MenuChangeEvent;
import com.qros.shared.exception.BusinessException;
import com.qros.shared.exception.ErrorCode;
import com.qros.shared.transaction.TransactionSideEffectService;
import com.qros.shared.validation.ImageFileValidator;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class MenuItemServiceTest {

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ComboItemRepository comboItemRepository;

    @Mock
    private MenuItemMapper menuItemMapper;

    @Mock
    private PublicMenuMapper publicMenuMapper;

    @Mock
    private StorageService storageService;

    @Mock
    private TransactionSideEffectService sideEffects;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private MenuItemOptionService menuItemOptionService;

    @Mock
    private ImageFileValidator imageFileValidator;

    @Mock
    private InventoryAvailabilityService inventoryAvailabilityService;

    private MenuItemService menuItemService;

    @BeforeEach
    void setUp() {
        menuItemService = new MenuItemService(
                menuItemRepository,
                categoryRepository,
                comboItemRepository,
                menuItemMapper,
                publicMenuMapper,
                storageService,
                sideEffects,
                eventPublisher,
                menuItemOptionService,
                imageFileValidator,
                inventoryAvailabilityService);
    }

    @Test
    void toggleAvailableWorksWithoutVersionAndSyncsCombos() {
        MenuItem item = MenuItem.builder()
                .id(7L)
                .name("Bún Bò Huế")
                .price(BigDecimal.valueOf(55000))
                .available(true)
                .version(4L)
                .category(Category.builder().id(2L).name("Món Nước").build())
                .build();

        MenuItemResponse expected = new MenuItemResponse(
                7L,
                "Bún Bò Huế",
                null,
                null,
                BigDecimal.valueOf(55000),
                true,
                false,
                0,
                null,
                List.of(),
                null,
                null,
                4L);

        when(menuItemRepository.findById(7L)).thenReturn(Optional.of(item));
        when(menuItemRepository.saveAndFlush(item)).thenReturn(item);
        when(menuItemMapper.toResponse(item)).thenReturn(expected);

        MenuItemResponse actual = menuItemService.toggleAvailable(7L, null);

        assertThat(actual.available()).isFalse();
        assertThat(item.getAvailable()).isFalse();
        verify(inventoryAvailabilityService).syncCombosForMenuItem(item);

        ArgumentCaptor<MenuChangeEvent> eventCaptor = ArgumentCaptor.forClass(MenuChangeEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().type()).isEqualTo("availability_updated");
        assertThat(eventCaptor.getValue().id()).isEqualTo(7L);
    }

    @Test
    void toggleAvailableRejectsReEnablingWhenInventoryStockIsInsufficient() {
        MenuItem item = MenuItem.builder()
                .id(9L)
                .name("Phở Bò")
                .available(false)
                .version(1L)
                .build();

        when(menuItemRepository.findById(9L)).thenReturn(Optional.of(item));
        when(inventoryAvailabilityService.getMenuItemAvailability(List.of(9L))).thenReturn(java.util.Map.of(9L, false));

        assertThatThrownBy(() -> menuItemService.toggleAvailable(9L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVENTORY_INSUFFICIENT_STOCK);
    }

    @Test
    void toggleAvailableRejectsMismatchedVersionWhenProvided() {
        MenuItem item = MenuItem.builder()
                .id(8L)
                .name("Cơm Tấm")
                .available(true)
                .version(5L)
                .build();

        when(menuItemRepository.findById(8L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> menuItemService.toggleAvailable(8L, 2L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CONCURRENT_MODIFICATION);
    }
}
