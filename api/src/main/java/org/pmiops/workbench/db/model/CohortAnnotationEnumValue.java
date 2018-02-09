package org.pmiops.workbench.db.model;

import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "cohort_annotation_enum_value")
public class CohortAnnotationEnumValue implements Comparable {

    private long cohortAnnotationEnumValueId;
    private String name;
    private int order;
    private CohortAnnotationDefinition cohortAnnotationDefinition;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cohort_annotation_enum_value_id")
    public long getCohortAnnotationEnumValueId() {
        return cohortAnnotationEnumValueId;
    }

    public void setCohortAnnotationEnumValueId(long cohortAnnotationEnumValueId) {
        this.cohortAnnotationEnumValueId = cohortAnnotationEnumValueId;
    }

    public CohortAnnotationEnumValue cohortAnnotationEnumValueId(long cohortAnnotationEnumValueId) {
        this.cohortAnnotationEnumValueId = cohortAnnotationEnumValueId;
        return this;
    }

    @Column(name = "name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CohortAnnotationEnumValue name(String name) {
        this.name = name;
        return this;
    }

    @Column(name = "enum_order")
    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public CohortAnnotationEnumValue order(int order) {
        this.order = order;
        return this;
    }

    @ManyToOne
    @JoinColumn(name = "cohort_annotation_definition_id")
    public CohortAnnotationDefinition getCohortAnnotationDefinition() {
        return cohortAnnotationDefinition;
    }

    public void setCohortAnnotationDefinition(CohortAnnotationDefinition cohortAnnotationDefinition) {
        this.cohortAnnotationDefinition = cohortAnnotationDefinition;
    }

    public CohortAnnotationEnumValue cohortAnnotationDefinition(CohortAnnotationDefinition cohortAnnotationDefinition) {
        this.cohortAnnotationDefinition = cohortAnnotationDefinition;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CohortAnnotationEnumValue enumValue = (CohortAnnotationEnumValue) o;
        return Objects.equals(name, enumValue.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("cohortAnnotationEnumValueId", cohortAnnotationEnumValueId)
                .append("name", name)
                .toString();
    }

    @Override
    public int compareTo(Object o) {
        Integer thisOrder = new Integer(this.getOrder());
        Integer otherOrder = new Integer(((CohortAnnotationEnumValue) o).getOrder());
        return thisOrder.compareTo(otherOrder);
    }
}
