package com.qros.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.qros.modules.analytics.service.DailySummaryRefreshService;
import com.qros.modules.menu.model.Category;
import com.qros.modules.menu.model.Combo;
import com.qros.modules.menu.model.ComboItem;
import com.qros.modules.menu.model.MenuItem;
import com.qros.modules.menu.repository.CategoryRepository;
import com.qros.modules.menu.repository.ComboRepository;
import com.qros.modules.menu.repository.MenuItemRepository;
import com.qros.modules.order.model.Order;
import com.qros.modules.order.model.OrderItem;
import com.qros.modules.order.model.enums.OrderItemStatus;
import com.qros.modules.order.model.enums.OrderItemType;
import com.qros.modules.order.model.enums.OrderStatus;
import com.qros.modules.order.model.enums.OrderType;
import com.qros.modules.order.model.enums.PaymentStatus;
import com.qros.modules.order.repository.OrderRepository;
import com.qros.modules.promotion.model.Voucher;
import com.qros.modules.promotion.model.enums.VoucherType;
import com.qros.modules.promotion.repository.VoucherRepository;
import com.qros.modules.settings.service.SystemSettingsService;
import com.qros.modules.user.model.User;
import com.qros.modules.user.model.enums.UserRole;
import com.qros.modules.user.model.enums.UserStatus;
import com.qros.modules.user.repository.UserRepository;
import com.qros.shared.enums.PaymentMethod;
import com.qros.shared.idempotency.IdempotencyRequestRepository;
import com.qros.shared.time.AppTime;
import com.redis.testcontainers.RedisContainer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers(disabledWithoutDocker = true)
class PostgresFlywayAndDomainIT {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("qros_it")
            .withUsername("qros_it")
            .withPassword("qros_it")
            .withEnv("TZ", "Asia/Ho_Chi_Minh")
            .withEnv("PGTZ", "Asia/Ho_Chi_Minh");

    @Container
    static final RedisContainer REDIS = new RedisContainer("redis:7-alpine");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.cache.type", () -> "redis");
        registry.add("spring.data.redis.host", REDIS::getRedisHost);
        registry.add("spring.data.redis.port", REDIS::getRedisPort);
        registry.add("spring.data.redis.ssl.enabled", () -> "false");
        registry.add("security.jwt.secret", () -> "integration-test-jwt-secret-key-with-at-least-32-bytes");
        registry.add("cloudinary.cloud_name", () -> "test-cloud");
        registry.add("cloudinary.api_key", () -> "test-key");
        registry.add("cloudinary.api_secret", () -> "test-secret");
        registry.add("payos.client-id", () -> "test-client-id");
        registry.add("payos.api-key", () -> "test-api-key");
        registry.add("payos.checksum-key", () -> "test-checksum-key");
        registry.add("app.base-url", () -> "https://api.qros.test");
        registry.add("app.frontend.base-url", () -> "https://app.qros.test");
        registry.add("app.security.qr-secret", () -> "integration-test-qr-secret-key-32-chars");
        registry.add("spring.mail.host", () -> "localhost");
        registry.add("management.health.mail.enabled", () -> "false");
        registry.add("spring.jpa.properties.hibernate.jdbc.time_zone", () -> "Asia/Ho_Chi_Minh");
    }

    @MockitoBean
    private JavaMailSender javaMailSender;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private ComboRepository comboRepository;

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private IdempotencyRequestRepository idempotencyRequestRepository;

    @Autowired
    private DailySummaryRefreshService dailySummaryRefreshService;

    @Autowired
    private SystemSettingsService systemSettingsService;

    @Test
    void flywayV1ToV126IndexesColumnWidthsAndCheckConstraintsAreEnforcedOnPostgres16() {
        List<String> droppedIndexes = List.of(
                "uk_category_name_active",
                "uk_menu_item_name_active",
                "uk_combos_name_active",
                "idx_menu_item_name_gin",
                "idx_category_name_gin");
        Integer duplicateCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM pg_indexes
                WHERE schemaname = 'public'
                  AND indexname IN (
                      'uk_category_name_active',
                      'uk_menu_item_name_active',
                      'uk_combos_name_active',
                      'idx_menu_item_name_gin',
                      'idx_category_name_gin'
                  )
                """, Integer.class);
        assertThat(duplicateCount)
                .as("Dropped duplicate indexes %s", droppedIndexes)
                .isZero();

        Integer factCreatedIdx = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM pg_indexes
                WHERE schemaname = 'public'
                  AND indexname = 'idx_order_reporting_fact_created_at_desc'
                """, Integer.class);
        assertThat(factCreatedIdx).isEqualTo(1);

        // ISSUE-023: users.avatar_url widened to VARCHAR(500) and full_name to VARCHAR(100)
        String longAvatarUrl = "https://res.cloudinary.com/demo/image/upload/" + "a".repeat(260) + ".png";
        String longFullName = "N".repeat(95);
        User user = User.builder()
                .email("long-avatar@qros.test")
                .password("$2a$10$abcdefghijklmnopqrstuv1234567890123456789012345678901")
                .fullName(longFullName)
                .avatarUrl(longAvatarUrl)
                .role(UserRole.STAFF)
                .status(UserStatus.ACTIVE)
                .build();
        User savedUser = userRepository.saveAndFlush(user);
        assertThat(savedUser.getAvatarUrl()).hasSizeGreaterThan(300);
        assertThat(savedUser.getFullName()).hasSize(95);

        // ISSUE-029: CHECK constraints reject negative unit_cost and out-of-range promotion percent
        assertThatThrownBy(() -> jdbcTemplate.update("""
                        INSERT INTO inventory_items (name, unit, quantity_on_hand, reserved_quantity, low_stock_threshold, unit_cost, active, is_deleted, version)
                        VALUES ('Bad Cost Item', 'kg', 10, 0, 1, -500, true, false, 0)
                        """)).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> jdbcTemplate.update("""
                        INSERT INTO promotions (name, discount_percent, start_time, end_time, active, is_deleted)
                        VALUES ('Invalid 150%', 150.00, '10:00:00', '12:00:00', true, false)
                        """)).isInstanceOf(DataIntegrityViolationException.class);

        // ISSUE-075: SystemSettings snapshot caches cleanly in Redis without Hibernate entity proxy errors
        assertThat(systemSettingsService.getSettings().restaurantName()).isNotBlank();
        assertThat(systemSettingsService.getSettingsEntity().getRestaurantName())
                .isNotBlank();
    }

    @Test
    void sqlDeleteWithVersionAndNotFoundIgnoreWorkOnRealPostgres() {
        Voucher voucher = Voucher.builder()
                .code("ITVOUCHER10")
                .type(VoucherType.PERCENTAGE)
                .discountPercent(BigDecimal.valueOf(10))
                .validFrom(AppTime.now().minusDays(1))
                .validTo(AppTime.now().plusDays(1))
                .active(true)
                .build();
        Voucher savedVoucher = voucherRepository.saveAndFlush(voucher);
        voucherRepository.delete(savedVoucher);
        voucherRepository.flush();
        assertThat(voucherRepository.findById(savedVoucher.getId())).isEmpty();

        Category category = categoryRepository.saveAndFlush(
                Category.builder().name("IT SoftDel Cat").active(true).build());
        MenuItem item = menuItemRepository.saveAndFlush(MenuItem.builder()
                .name("IT SoftDel Pho")
                .price(BigDecimal.valueOf(55000))
                .available(true)
                .category(category)
                .build());

        Order order = Order.builder()
                .orderType(OrderType.TAKEAWAY)
                .status(OrderStatus.SERVING)
                .paymentStatus(PaymentStatus.PENDING)
                .subtotalAmount(BigDecimal.valueOf(55000))
                .discountAmount(BigDecimal.valueOf(8250))
                .appliedPromotionPercent(BigDecimal.valueOf(15))
                .finalAmount(BigDecimal.valueOf(46750))
                .build();
        OrderItem orderItem = OrderItem.builder()
                .itemType(OrderItemType.MENU_ITEM)
                .menuItem(item)
                .itemNameSnapshot("IT SoftDel Pho")
                .quantity(1)
                .unitPrice(BigDecimal.valueOf(55000))
                .lineTotal(BigDecimal.valueOf(55000))
                .status(OrderItemStatus.PENDING)
                .build();
        order.addItem(orderItem);
        Order savedOrder = orderRepository.saveAndFlush(order);

        // Soft-delete referenced menu item, then read historical order cleanly via @NotFound(IGNORE)
        menuItemRepository.delete(item);
        menuItemRepository.flush();

        Order reloaded = transactionTemplate.execute(
                status -> orderRepository.findDetailById(savedOrder.getId()).orElseThrow());
        assertThat(reloaded.getAppliedPromotionPercent()).isEqualByComparingTo("15.00");
        assertThat(reloaded.getOrderItems()).hasSize(1);
        assertThat(reloaded.getOrderItems().iterator().next().getItemNameSnapshot())
                .isEqualTo("IT SoftDel Pho");

        // Soft-delete Order (2-param @SQLDelete id + version)
        orderRepository.delete(reloaded);
        orderRepository.flush();
        assertThat(orderRepository.findById(savedOrder.getId())).isEmpty();
    }

    @Test
    void reportingTriggerAndDailySummaryPopulateComboFallbackThumbnail() {
        Category category = categoryRepository.saveAndFlush(
                Category.builder().name("IT Combo Cat").active(true).build());
        String expectedThumb = "https://res.cloudinary.com/demo/image/upload/v1/pho-combo-thumb.jpg";
        MenuItem pho = menuItemRepository.saveAndFlush(MenuItem.builder()
                .name("IT Pho Dac Biet")
                .price(BigDecimal.valueOf(65000))
                .img(expectedThumb)
                .available(true)
                .category(category)
                .build());
        Combo combo = Combo.builder()
                .name("IT Combo Sang")
                .price(BigDecimal.valueOf(89000))
                .active(true)
                .available(true)
                .build();
        combo.getItems()
                .add(ComboItem.builder().combo(combo).menuItem(pho).quantity(1).build());
        Combo savedCombo = comboRepository.saveAndFlush(combo);

        Order paidOrder = Order.builder()
                .orderType(OrderType.TAKEAWAY)
                .status(OrderStatus.COMPLETED)
                .paymentStatus(PaymentStatus.PAID)
                .paymentMethod(PaymentMethod.CASH)
                .subtotalAmount(BigDecimal.valueOf(89000))
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(BigDecimal.valueOf(89000))
                .paidAmount(BigDecimal.valueOf(89000))
                .build();
        paidOrder.addItem(OrderItem.builder()
                .itemType(OrderItemType.COMBO)
                .combo(savedCombo)
                .itemNameSnapshot("IT Combo Sang")
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(89000))
                .lineTotal(BigDecimal.valueOf(178000))
                .status(OrderItemStatus.FINISHED)
                .build());
        Order savedOrder = orderRepository.saveAndFlush(paidOrder);

        LocalDate businessDate = jdbcTemplate.queryForObject(
                "SELECT business_date FROM order_reporting_fact WHERE order_id = ?",
                LocalDate.class,
                savedOrder.getId());
        assertThat(businessDate).isNotNull();

        dailySummaryRefreshService.refreshDate(businessDate);

        Map<String, Object> comboSummary = jdbcTemplate.queryForMap("""
                SELECT item_name_snapshot, image_url_snapshot, quantity
                FROM daily_item_sales_summary
                WHERE business_date = ? AND item_type = 'COMBO' AND combo_id = ?
                """, businessDate, savedCombo.getId());
        assertThat(comboSummary.get("item_name_snapshot")).isEqualTo("IT Combo Sang");
        assertThat(comboSummary.get("image_url_snapshot")).isEqualTo(expectedThumb);
        assertThat(((Number) comboSummary.get("quantity")).longValue()).isEqualTo(2L);
    }

    @Test
    void idempotencyClaimBlocksActiveDuplicateAndReclaimsExpiredKeyImmediately() {
        LocalDateTime now = AppTime.now();
        Integer firstClaim = transactionTemplate.execute(status ->
                idempotencyRequestRepository.claim("ORDER_IT", "idem-key-1", "hash-v1", now, now.plusMinutes(10)));
        assertThat(firstClaim).isEqualTo(1);

        Integer activeDuplicate = transactionTemplate.execute(status -> idempotencyRequestRepository.claim(
                "ORDER_IT", "idem-key-1", "hash-v1", now.plusMinutes(1), now.plusMinutes(11)));
        assertThat(activeDuplicate).isZero();

        // Advance createdAt past expires_at -> ON CONFLICT DO UPDATE WHERE expires_at <= EXCLUDED.created_at reclaims
        LocalDateTime afterExpiry = now.plusMinutes(15);
        Integer reclaimed = transactionTemplate.execute(status -> idempotencyRequestRepository.claim(
                "ORDER_IT", "idem-key-1", "hash-v2", afterExpiry, afterExpiry.plusMinutes(10)));
        assertThat(reclaimed).isEqualTo(1);

        String storedHash = jdbcTemplate.queryForObject(
                "SELECT request_hash FROM idempotency_requests WHERE namespace = 'ORDER_IT' AND request_key = 'idem-key-1'",
                String.class);
        assertThat(storedHash).isEqualTo("hash-v2");
    }
}
