package com.sacmauquan.qrordering.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;
import java.util.LinkedHashSet;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * ItemOption - Entity representing a customizable choice for a menu item (e.g., Size, Toppings).
 */
@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "item_option")
@SQLDelete(sql = "UPDATE item_option SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class ItemOption extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Display name of the option group (e.g., "Ice Level").
     */
    @NotBlank(message = "Option name cannot be empty")
    @Column(length = 50, nullable = false)
    private String name;

    /**
     * Indicates if the customer must select at least one value from this option.
     */
    @Builder.Default
    @Column(nullable = false)
    @JsonProperty("isRequired")
    private boolean isRequired = false;

    /**
     * Maximum number of values that can be selected from this option group.
     */
    @Builder.Default
    @Column(nullable = false)
    @Min(value = 1, message = "Max selection must be at least 1")
    private int maxSelection = 1;

    /**
     * The menu item this option belongs to.
     */
    @NotNull(message = "Menu item cannot be empty")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id")
    @JsonIgnoreProperties("itemOptions")
    private MenuItem menuItem;

    /**
     * Collection of possible values within this option group (e.g., "No Ice", "Less Ice").
     */
    @Builder.Default
    @OneToMany(mappedBy = "itemOption", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({ "itemOption" })
    private Set<ItemOptionValue> optionValues = new LinkedHashSet<>();
}
