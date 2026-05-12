package com.sacmauquan.qrordering.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Combo - Entity representing a promotional package containing multiple menu items at a fixed price.
 */
@Entity
@Table(name = "combos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@SQLDelete(sql = "UPDATE combos SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Combo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique display name of the combo package.
     */
    @NotBlank(message = "Combo name cannot be empty")
    @Column(length = 100, nullable = false, unique = true)
    private String name;

    /**
     * Total price of the combo package.
     */
    @Column(nullable = false, precision = 15, scale = 2)
    @Min(0)
    private BigDecimal price;

    /**
     * Flag indicating if the combo is currently available for sale.
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    /**
     * Collection of individual menu items included in this combo package.
     */
    @Builder.Default
    @OneToMany(mappedBy = "combo", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("combo")
    private Set<ComboItem> items = new LinkedHashSet<>();
}
