package com.valueplus.persistence.entity;

import com.valueplus.domain.model.ProductModel;
import lombok.*;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Builder
@NoArgsConstructor
@Setter
@Getter
@AllArgsConstructor
@Table(name = "product")
@Accessors(chain = true)
public class Product extends BasePersistentEntity implements ToModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true)
    private String name;
    @Column(nullable = false)
    private String description;
    @Column(nullable = false)
    private BigDecimal price;
    @Column(nullable = false)
    private String image;
    @Column(nullable = false)
    private boolean deleted;
    @Column(nullable = false)
    private boolean disabled;
    @Column(name = "isDiscounted", columnDefinition = "boolean default false")
    private boolean discounted=false;
    private BigDecimal discountPrice;
    private String discountPercentage;

    @ManyToOne
    @JoinColumn(name = "sub_category_id")
    private BusinessSubcategory businessSubcategory;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private BusinessCategory businessCategory;


    public ProductModel toModel() {
        return ProductModel.builder()
                .id(this.id)
                .name(this.name)
                .description(this.description)
                .price(this.price)
                .image(this.image)
                .categoryName(this.businessCategory.getName())
                .subcategoryName(this.businessSubcategory.getName())
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .disabled(this.disabled)
                .discountPercentage(this.getDiscountPercentage())
                .discounted(this.isDiscounted())
                .discountPrice(this.discountPrice)
                .build();
    }

    public static Product fromModel(ProductModel model) {
        return Product.builder()
                .name(model.getName())
                .description(model.getDescription())
                .price(model.getPrice())
                .image(model.getImage())
                .discountPercentage(model.getDiscountPercentage())
                .discounted(model.isDiscounted())
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Product product = (Product) o;
        return id.equals(product.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
