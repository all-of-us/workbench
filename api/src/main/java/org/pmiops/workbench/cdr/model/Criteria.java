package org.pmiops.workbench.cdr.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;


@Entity
@Table(name = "criteria", catalog="cdr")
public class Criteria {

    private long id;
    private long sortOrder;
    private long parentId;
    private String type;
    private String code;
    private String name;
    private boolean group;
    private boolean selectable;
    private String count;
    private String conceptId;
    private String domainId;

    @Id
    @GeneratedValue
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

    @Column(name = "sort_order")
    public long getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(long sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Criteria sortOrder(long sortOrder) {
        this.sortOrder = sortOrder;
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

        if (id != criteria.id) return false;
        if (sortOrder != criteria.sortOrder) return false;
        if (parentId != criteria.parentId) return false;
        if (group != criteria.group) return false;
        if (selectable != criteria.selectable) return false;
        if (type != null ? !type.equals(criteria.type) : criteria.type != null) return false;
        if (code != null ? !code.equals(criteria.code) : criteria.code != null) return false;
        if (name != null ? !name.equals(criteria.name) : criteria.name != null) return false;
        if (count != null ? !count.equals(criteria.count) : criteria.count != null) return false;
        if (conceptId != null ? !conceptId.equals(criteria.conceptId) : criteria.conceptId != null) return false;
        return domainId != null ? domainId.equals(criteria.domainId) : criteria.domainId == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (int) (sortOrder ^ (sortOrder >>> 32));
        result = 31 * result + (int) (parentId ^ (parentId >>> 32));
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (code != null ? code.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (group ? 1 : 0);
        result = 31 * result + (selectable ? 1 : 0);
        result = 31 * result + (count != null ? count.hashCode() : 0);
        result = 31 * result + (conceptId != null ? conceptId.hashCode() : 0);
        result = 31 * result + (domainId != null ? domainId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Criteria{");
        sb.append("id=").append(id);
        sb.append(", sortOrder=").append(sortOrder);
        sb.append(", parentId=").append(parentId);
        sb.append(", type='").append(type).append('\'');
        sb.append(", code='").append(code).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", group=").append(group);
        sb.append(", selectable=").append(selectable);
        sb.append(", count='").append(count).append('\'');
        sb.append(", conceptId='").append(conceptId).append('\'');
        sb.append(", domainId='").append(domainId).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
