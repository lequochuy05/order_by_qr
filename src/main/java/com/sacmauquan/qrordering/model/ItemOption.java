package com.sacmauquan.qrordering.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;
import java.util.LinkedHashSet;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

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

    @NotBlank(message = "Tên lựa chọn không được để trống")
    @Column(length = 50, nullable = false)
    private String name;

    @Builder.Default
    @Column(nullable = false)
    @JsonProperty("isRequired")
    private boolean isRequired = false;

    @Builder.Default
    @Column(nullable = false)
    @Min(1)
    private int maxSelection = 1;

    @NotNull(message = "Món ăn không được để trống")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id")
    private MenuItem menuItem;

    @Builder.Default
    @OneToMany(mappedBy = "itemOption", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({ "itemOption" })
    private Set<ItemOptionValue> optionValues = new LinkedHashSet<>();
}
