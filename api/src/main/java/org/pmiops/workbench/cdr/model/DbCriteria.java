package org.pmiops.workbench.cdr.model;

import com.google.common.base.Strings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.Objects;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Entity
@Table(name = "cb_criteria")
public class DbCriteria {

  private long id;
  private long parentId;
  private String type;
  private String subtype;
  private String code;
  private String name;
  private boolean group;
  private boolean selectable;
  private Long count;
  private Long parentCount;
  private Long childCount;
  private String conceptId;
  private String domainId;
  private boolean attribute;
  private String path;
  private String synonyms;
  private String fullText;
  private String value;
  private boolean hierarchy;
  private boolean ancestorData;
  private boolean standard;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
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

  @Column(name = "subtype")
  public String getSubtype() {
    return subtype;
  }

  public void setSubtype(String subtype) {
    this.subtype = subtype;
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
  public Long getCount() {
    return this.count == null ? 0 : this.count;
  }

  public void setCount(Long count) {
    this.count = count;
  }

  @Column(name = "rollup_count")
  public Long getParentCount() {
    return this.parentCount == null ? 0 : this.parentCount;
  }

  public void setParentCount(Long parentCount) {
    this.parentCount = parentCount;
  }

  @Column(name = "item_count")
  public Long getChildCount() {
    return this.childCount == null ? 0 : this.childCount;
  }

  public void setChildCount(Long childCount) {
    this.childCount = childCount;
  }

  @Column(name = "concept_id")
  public String getConceptId() {
    return conceptId;
  }

  public void setConceptId(String conceptId) {
    this.conceptId = conceptId;
  }

  @Transient
  public Long getLongConceptId() {
    return Strings.isNullOrEmpty(this.conceptId) ? null : Long.valueOf(this.conceptId);
  }

  @Column(name = "domain_id")
  public String getDomainId() {
    return domainId;
  }

  public void setDomainId(String domainId) {
    this.domainId = domainId;
  }

  @Column(name = "has_attribute")
  public boolean getAttribute() {
    return attribute;
  }

  public void setAttribute(boolean attribute) {
    this.attribute = attribute;
  }

  @Column(name = "path")
  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  @Column(name = "synonyms")
  public String getSynonyms() {
    return synonyms;
  }

  public void setSynonyms(String synonyms) {
    this.synonyms = synonyms;
  }

  @Column(name = "full_text")
  public String getFullText() {
    return fullText;
  }

  public void setFullText(String fullText) {
    this.fullText = fullText;
  }

  @Column(name = "value")
  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  @Column(name = "has_hierarchy")
  public boolean getHierarchy() {
    return hierarchy;
  }

  public void setHierarchy(boolean hierarchy) {
    this.hierarchy = hierarchy;
  }

  @Column(name = "has_ancestor_data")
  public boolean getAncestorData() {
    return ancestorData;
  }

  public void setAncestorData(boolean ancestorData) {
    this.ancestorData = ancestorData;
  }

  @Column(name = "is_standard")
  public boolean isStandard() {
    return standard;
  }

  public void setStandard(boolean standard) {
    this.standard = standard;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DbCriteria criteria = (DbCriteria) o;
    return parentId == criteria.parentId
        && group == criteria.group
        && selectable == criteria.selectable
        && Objects.equals(type, criteria.type)
        && Objects.equals(code, criteria.code)
        && Objects.equals(name, criteria.name)
        && Objects.equals(count, criteria.count)
        && Objects.equals(parentCount, criteria.parentCount)
        && Objects.equals(childCount, criteria.childCount)
        && Objects.equals(conceptId, criteria.conceptId)
        && Objects.equals(domainId, criteria.domainId)
        && Objects.equals(attribute, criteria.attribute)
        && Objects.equals(path, criteria.path)
        && Objects.equals(value, criteria.value)
        && Objects.equals(hierarchy, criteria.hierarchy)
        && Objects.equals(ancestorData, criteria.ancestorData)
        && Objects.equals(standard, criteria.standard);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        id,
        parentId,
        type,
        code,
        name,
        group,
        selectable,
        count,
        parentCount,
        childCount,
        conceptId,
        domainId,
        attribute,
        path,
        value,
        hierarchy,
        ancestorData,
        standard);
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private long id;
    private long parentId;
    private String type;
    private String subtype;
    private String code;
    private String name;
    private boolean group;
    private boolean selectable;
    private Long count;
    private Long parentCount;
    private Long childCount;
    private String conceptId;
    private String domainId;
    private boolean attribute;
    private String path;
    private String synonyms;
    private String fullText;
    private String value;
    private boolean hierarchy;
    private boolean ancestorData;
    private boolean standard;

    private Builder() {}

    public Builder addId(long id) {
      this.id = id;
      return this;
    }

    public Builder addParentId(long parentId) {
      this.parentId = parentId;
      return this;
    }

    public Builder addType(String type) {
      this.type = type;
      return this;
    }

    public Builder addSubtype(String subtype) {
      this.subtype = subtype;
      return this;
    }

    public Builder addCode(String code) {
      this.code = code;
      return this;
    }

    public Builder addName(String name) {
      this.name = name;
      return this;
    }

    public Builder addGroup(boolean group) {
      this.group = group;
      return this;
    }

    public Builder addSelectable(boolean selectable) {
      this.selectable = selectable;
      return this;
    }

    public Builder addCount(Long count) {
      this.count = count;
      return this;
    }

    public Builder addParentCount(Long parentCount) {
      this.parentCount = parentCount;
      return this;
    }

    public Builder addChildCount(Long childCount) {
      this.childCount = childCount;
      return this;
    }

    public Builder addConceptId(String conceptId) {
      this.conceptId = conceptId;
      return this;
    }

    public Builder addDomainId(String domainId) {
      this.domainId = domainId;
      return this;
    }

    public Builder addAttribute(boolean attribute) {
      this.attribute = attribute;
      return this;
    }

    public Builder addPath(String path) {
      this.path = path;
      return this;
    }

    public Builder addSynonyms(String synonyms) {
      this.synonyms = synonyms;
      return this;
    }

    public Builder addFullText(String fullText) {
      this.fullText = fullText;
      return this;
    }

    public Builder addValue(String value) {
      this.value = value;
      return this;
    }

    public Builder addHierarchy(boolean hierarchy) {
      this.hierarchy = hierarchy;
      return this;
    }

    public Builder addAncestorData(boolean ancestorData) {
      this.ancestorData = ancestorData;
      return this;
    }

    public Builder addStandard(boolean standard) {
      this.standard = standard;
      return this;
    }

    public DbCriteria build() {
      DbCriteria dbCriteria = new DbCriteria();
      dbCriteria.setId(this.id);
      dbCriteria.setParentId(this.parentId);
      dbCriteria.setType(this.type);
      dbCriteria.setSubtype(this.subtype);
      dbCriteria.setCode(this.code);
      dbCriteria.setName(this.name);
      dbCriteria.setGroup(this.group);
      dbCriteria.setSelectable(this.selectable);
      dbCriteria.setCount(this.count);
      dbCriteria.setParentCount(this.parentCount);
      dbCriteria.setChildCount(this.childCount);
      dbCriteria.setConceptId(this.conceptId);
      dbCriteria.setDomainId(this.domainId);
      dbCriteria.setAttribute(this.attribute);
      dbCriteria.setPath(this.path);
      dbCriteria.setSynonyms(this.synonyms);
      dbCriteria.setFullText(this.fullText);
      dbCriteria.setValue(this.value);
      dbCriteria.setHierarchy(this.hierarchy);
      dbCriteria.setAncestorData(this.ancestorData);
      dbCriteria.setStandard(this.standard);
      return dbCriteria;
    }
  }
}
