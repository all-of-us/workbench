package org.pmiops.workbench.cdr.model;

import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Entity
@Table(name = "cb_menu")
public class DbCBMenu {
  private long id;
  private long parentId;
  private String category;
  private String domainId;
  private String type;
  private String name;
  private boolean group;
  private boolean standard;
  private long sortOrder;

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

  @Column(name = "category")
  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  @Column(name = "domain_id")
  public String getDomainId() {
    return domainId;
  }

  public void setDomainId(String domainId) {
    this.domainId = domainId;
  }

  @Column(name = "type")
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
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

  @Column(name = "is_standard")
  public boolean getStandard() {
    return standard;
  }

  public void setStandard(boolean standard) {
    this.standard = standard;
  }

  @Column(name = "sort_order")
  public long getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(long sortOrder) {
    this.sortOrder = sortOrder;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DbCBMenu that = (DbCBMenu) o;
    return ((parentId == that.parentId)
        && (group == that.group)
        && (standard = that.standard)
        && (sortOrder == that.sortOrder)
        && Objects.equals(category, that.category)
        && Objects.equals(domainId, that.domainId)
        && Objects.equals(type, that.type)
        && Objects.equals(name, that.name));
  }

  @Override
  public int hashCode() {
    return Objects.hash(parentId, category, domainId, type, name, group, standard, sortOrder);
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  public static DbCBMenu.Builder builder() {
    return new DbCBMenu.Builder();
  }

  public static class Builder {
    private long id;
    private long parentId;
    private String category;
    private String domainId;
    private String type;
    private String name;
    private boolean group;
    private boolean isStandard;
    private long sortOrder;

    private Builder() {}

    public DbCBMenu.Builder addId(long id) {
      this.id = id;
      return this;
    }

    public DbCBMenu.Builder addParentId(long parentId) {
      this.parentId = parentId;
      return this;
    }

    public DbCBMenu.Builder addCategory(String category) {
      this.category = category;
      return this;
    }

    public DbCBMenu.Builder addDomainId(String domainId) {
      this.domainId = domainId;
      return this;
    }

    public DbCBMenu.Builder addType(String type) {
      this.type = type;
      return this;
    }

    public DbCBMenu.Builder addName(String name) {
      this.name = name;
      return this;
    }

    public DbCBMenu.Builder addGroup(boolean group) {
      this.group = group;
      return this;
    }

    public DbCBMenu.Builder addIsStandard(boolean isStandard) {
      this.isStandard = isStandard;
      return this;
    }

    public DbCBMenu.Builder addSortOrder(long sortOrder) {
      this.sortOrder = sortOrder;
      return this;
    }

    public DbCBMenu build() {
      DbCBMenu DbCBMenu = new DbCBMenu();
      DbCBMenu.setId(this.id);
      DbCBMenu.setParentId(this.parentId);
      DbCBMenu.setCategory(this.category);
      DbCBMenu.setDomainId(this.domainId);
      DbCBMenu.setType(this.type);
      DbCBMenu.setName(this.name);
      DbCBMenu.setGroup(this.group);
      DbCBMenu.setStandard(this.isStandard);
      DbCBMenu.setSortOrder(this.sortOrder);
      return DbCBMenu;
    }
  }
}
