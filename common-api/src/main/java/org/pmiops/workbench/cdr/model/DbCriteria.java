package org.pmiops.workbench.cdr.model;

import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.util.StringUtils;

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
  private String count;
  private String conceptId;
  private String domainId;
  private boolean attribute;
  private String path;
  private String synonyms;
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

  public DbCriteria id(long id) {
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

  public DbCriteria parentId(long parentId) {
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

  public DbCriteria subtype(String subtype) {
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

  public DbCriteria type(String type) {
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

  public DbCriteria code(String code) {
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

  public DbCriteria name(String name) {
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

  public DbCriteria group(boolean group) {
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

  public DbCriteria selectable(boolean selectable) {
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

  public DbCriteria count(String count) {
    this.count = count;
    return this;
  }

  @Transient
  public long getLongCount() {
    return StringUtils.isEmpty(this.count) ? null : Long.valueOf(this.count);
  }

  @Column(name = "concept_id")
  public String getConceptId() {
    return conceptId;
  }

  public void setConceptId(String conceptId) {
    this.conceptId = conceptId;
  }

  public DbCriteria conceptId(String conceptId) {
    this.conceptId = conceptId;
    return this;
  }

  @Transient
  public long getLongConceptId() {
    return StringUtils.isEmpty(this.conceptId) ? null : Long.valueOf(this.conceptId);
  }

  @Column(name = "domain_id")
  public String getDomainId() {
    return domainId;
  }

  public void setDomainId(String domainId) {
    this.domainId = domainId;
  }

  public DbCriteria domainId(String domainId) {
    this.domainId = domainId;
    return this;
  }

  @Column(name = "has_attribute")
  public boolean getAttribute() {
    return attribute;
  }

  public void setAttribute(boolean attribute) {
    this.attribute = attribute;
  }

  public DbCriteria attribute(boolean attribute) {
    this.attribute = attribute;
    return this;
  }

  @Column(name = "path")
  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public DbCriteria path(String path) {
    this.path = path;
    return this;
  }

  @Column(name = "synonyms")
  public String getSynonyms() {
    return synonyms;
  }

  public void setSynonyms(String synonyms) {
    this.synonyms = synonyms;
  }

  public DbCriteria synonyms(String synonyms) {
    this.synonyms = synonyms;
    return this;
  }

  @Column(name = "value")
  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public DbCriteria value(String value) {
    this.value = value;
    return this;
  }

  @Column(name = "has_hierarchy")
  public boolean getHierarchy() {
    return hierarchy;
  }

  public void setHierarchy(boolean hierarchy) {
    this.hierarchy = hierarchy;
  }

  public DbCriteria hierarchy(boolean hierarchy) {
    this.hierarchy = hierarchy;
    return this;
  }

  @Column(name = "has_ancestor_data")
  public boolean getAncestorData() {
    return ancestorData;
  }

  public void setAncestorData(boolean ancestorData) {
    this.ancestorData = ancestorData;
  }

  public DbCriteria ancestorData(boolean ancestorData) {
    this.ancestorData = ancestorData;
    return this;
  }

  @Column(name = "is_standard")
  public boolean getStandard() {
    return standard;
  }

  public void setStandard(boolean standard) {
    this.standard = standard;
  }

  public DbCriteria standard(boolean standard) {
    this.standard = standard;
    return this;
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
}
