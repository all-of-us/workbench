package org.pmiops.workbench.cdr.model;

import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.model.Domain;

@Entity
@Table(name = "domain_card")
public class DbDomainCard {
  private long id;
  private String category;
  private short domain;
  private String name;
  private String description;
  private long conceptCount;
  private long participantCount;
  private boolean standard;
  private long sortOrder;

  @Id
  @Column(name = "id")
  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public DbDomainCard id(Long id) {
    this.id = id;
    return this;
  }

  @Column(name = "category")
  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public DbDomainCard category(String category) {
    this.setCategory(category);
    return this;
  }

  @Column(name = "domain")
  public short getDomain() {
    return domain;
  }

  public void setDomain(short domain) {
    this.domain = domain;
  }

  public DbDomainCard domain(short domain) {
    this.setDomain(domain);
    return this;
  }

  @Transient
  public Domain getDomainEnum() {
    return DbStorageEnums.domainFromStorage(domain);
  }

  public DbDomainCard domainEnum(Domain domain) {
    this.domain = DbStorageEnums.domainToStorage(domain);
    return this;
  }

  @Column(name = "name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public DbDomainCard name(String name) {
    this.setName(name);
    return this;
  }

  @Column(name = "description")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public DbDomainCard description(String description) {
    this.setDescription(description);
    return this;
  }

  @Column(name = "concept_count")
  public long getConceptCount() {
    return conceptCount;
  }

  public void setConceptCount(long conceptCount) {
    this.conceptCount = conceptCount;
  }

  public DbDomainCard conceptCount(long conceptCount) {
    this.setConceptCount(conceptCount);
    return this;
  }

  @Column(name = "participant_count")
  public long getParticipantCount() {
    return participantCount;
  }

  public void setParticipantCount(long participantCount) {
    this.participantCount = participantCount;
  }

  public DbDomainCard participantCount(long participantCount) {
    this.setParticipantCount(participantCount);
    return this;
  }

  @Column(name = "is_standard")
  public boolean isStandard() {
    return standard;
  }

  public void setStandard(boolean standard) {
    this.standard = standard;
  }

  public DbDomainCard standard(boolean standard) {
    this.setStandard(standard);
    return this;
  }

  @Column(name = "sort_order")
  public long getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(long sortOrder) {
    this.sortOrder = sortOrder;
  }

  public DbDomainCard sortOrder(long sortOrder) {
    this.setSortOrder(sortOrder);
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DbDomainCard that = (DbDomainCard) o;
    return id == that.id
        && domain == that.domain
        && conceptCount == that.conceptCount
        && participantCount == that.participantCount
        && standard == that.standard
        && sortOrder == that.sortOrder
        && category.equals(that.category)
        && name.equals(that.name)
        && description.equals(that.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        id,
        category,
        domain,
        name,
        description,
        conceptCount,
        participantCount,
        standard,
        sortOrder);
  }
}
