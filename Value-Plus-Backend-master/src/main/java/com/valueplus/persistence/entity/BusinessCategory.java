package com.valueplus.persistence.entity;

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
@Table(name="business_category")
@NamedQuery(name="BusinessCategory.findAll", query="SELECT b FROM BusinessCategory b")
public class BusinessCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;

    //bi-directional many-to-one association to BusinessSubcategory
    @OneToMany(mappedBy="businessCategory")
    private List<BusinessSubcategory> businessSubcategories;

    @Column(name="is_deleted")
    private boolean deleted;

    private String photo;

}
