package com.valueplus.persistence.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.List;
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Entity
@Table(name="business_subcategory")
@NamedQuery(name="BusinessSubcategory.findAll", query="SELECT b FROM BusinessSubcategory b")
public class BusinessSubcategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;


    @Column(name="category_id", insertable = false, updatable = false)
    private int categoryId;

    @Column(name="name")
    private String name;


    //bi-directional many-to-one association to BusinessCategory
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name="category_id")
    private BusinessCategory businessCategory;

    //bi-directional many-to-one association to Product
    @JsonIgnore
    @OneToMany(mappedBy="businessSubcategory")
    private List<Product> products;

    @Column(name="is_deleted")
    private boolean deleted;

}
