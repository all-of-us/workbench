package org.pmiops.workbench.cdr.model;

import javax.persistence.GenerationType;
import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Objects;


@Entity
//TODO need to add a way to dynamically switch between database versions
//this dynamic connection will eliminate the need for the catalog attribute
@Table(name = "criteria")
public class Criteria {

    private long id;
    private long parentId;
    private String type;
    private String subtype;
    private String code;
    private String name;
    private boolean group;
    private boolean selectable;
    private String count;
    private String conceptId;
    private String domainId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Criteria id(long id) {
        this.id = id;
        return this;
    }

    @Column(name = "parent_id")
    public long getParentId() {
        return parentId;
    }

    public void setParentId(long parentId) {
        this.parentId = parentId;
    }

    public Criteria parentId(long parentId) {
        this.parentId = parentId;
        return this;
    }

    @Column(name = "subtype")
    public String getSubtype() {
        return subtype;
    }

    public void setSubtype(String subtype) {
        this.subtype = subtype;
    }

    public Criteria subtype(String subtype) {
        this.subtype = subtype;
        return this;
    }

    @Column(name = "type")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Criteria type(String type) {
        this.type = type;
        return this;
    }

    @Column(name = "code")
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Criteria code(String code) {
        this.code = code;
        return this;
    }

    @Column(name = "name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Criteria name(String name) {
        this.name = name;
        return this;
    }

    @Column(name = "is_group")
    public boolean getGroup() {
        return group;
    }

    public void setGroup(boolean group) {
        this.group = group;
    }

    public Criteria group(boolean group) {
        this.group = group;
        return this;
    }

    @Column(name = "is_selectable")
    public boolean getSelectable() {
        return selectable;
    }

    public void setSelectable(boolean selectable) {
        this.selectable = selectable;
    }

    public Criteria selectable(boolean selectable) {
        this.selectable = selectable;
        return this;
    }

    @Column(name = "est_count")
    public String getCount() {
        return count;
    }

    public void setCount(String count) {
        this.count = count;
    }

    public Criteria count(String count) {
        this.count = count;
        return this;
    }

    @Column(name = "concept_id")
    public String getConceptId() {
        return conceptId;
    }

    public void setConceptId(String conceptId) {
        this.conceptId = conceptId;
    }

    public Criteria conceptId(String conceptId) {
        this.conceptId = conceptId;
        return this;
    }

    @Column(name = "domain_id")
    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public Criteria domainId(String domainId) {
        this.domainId = domainId;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Criteria criteria = (Criteria) o;
        return id == criteria.id &&
                parentId == criteria.parentId &&
                group == criteria.group &&
                selectable == criteria.selectable &&
                Objects.equals(type, criteria.type) &&
                Objects.equals(code, criteria.code) &&
                Objects.equals(name, criteria.name) &&
                Objects.equals(count, criteria.count) &&
                Objects.equals(conceptId, criteria.conceptId) &&
                Objects.equals(domainId, criteria.domainId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, parentId, type, code, name, group, selectable, count, conceptId, domainId);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
