package org.pmiops.workbench.cdr.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;


@Entity
@Table(name = "icd9_criteria", catalog="cdr")
public class Icd9Criteria {

    private long id;
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
    @Column(name = "id")
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Column(name = "parent_id")
    public long getParentId() {
        return parentId;
    }

    public void setParentId(long parentId) {
        this.parentId = parentId;
    }

    @Column(name = "type")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Column(name = "code")
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Column(name = "name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(name = "is_group")
    public boolean getGroup() {
        return group;
    }

    public void setGroup(boolean group) {
        this.group = group;
    }

    @Column(name = "is_selectable")
    public boolean getSelectable() {
        return selectable;
    }

    public void setSelectable(boolean selectable) {
        this.selectable = selectable;
    }

    @Column(name = "est_count")
    public String getCount() {
        return count;
    }

    public void setCount(String count) {
        this.count = count;
    }

    @Column(name = "concept_id")
    public String getConceptId() {
        return conceptId;
    }

    public void setConceptId(String conceptId) {
        this.conceptId = conceptId;
    }

    @Column(name = "domain_id")
    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }
}
