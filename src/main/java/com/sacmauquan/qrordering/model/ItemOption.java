package com.sacmauquan.qrordering.model;

import jakarta.persistence.*;
import lombok.experimental.SuperBuilder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.List;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "item_option")
@SQLDelete(sql = "UPDATE item_option SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class ItemOption extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Builder.Default
    @JsonProperty("isRequired")
    private boolean isRequired = false;

    @Builder.Default
    private int maxSelection = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id")
    private MenuItem menuItem;

    @Builder.Default
    @OneToMany(mappedBy = "itemOption", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({ "itemOption" })
    private Set<ItemOptionValue> optionValues = new LinkedHashSet<>();
}
