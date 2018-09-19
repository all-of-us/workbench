package org.pmiops.workbench.cdr.model;

import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.persistence.Basic;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.pmiops.workbench.db.model.CommonStorageEnums;
import org.pmiops.workbench.model.Domain;

@Entity
@Table(name = "domain_info")
public class DomainInfo {

  private long conceptId;
  private short domain;
  private String name;
  private String description;
  private long allConceptCount;
  private long standardConceptCount;
  private long participantCount;

  @Id
  @Column(name = "concept_id")
  public Long getConceptId() {
    return conceptId;
  }

  public void setConceptId(Long conceptId) {
    this.conceptId = conceptId;
  }

  public DomainInfo conceptId(Long conceptId) {
    this.conceptId = conceptId;
    return this;
  }

  @Column(name = "name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Column(name = "domain")
  public short getDomain() {
    return domain;
  }

  public void setDomain(short domain) {
    this.domain = domain;
  }

  @Transient
  public Domain getDomainEnum() {
    return CommonStorageEnums.domainFromStorage(domain);
  }

  public void setDomainEnum(Domain domain) {
    this.domain = CommonStorageEnums.domainToStorage(domain);
  }

  @Column(name = "description")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Column(name="all_concept_count")
  public long getAllConceptCount() {
    return allConceptCount;
  }

  public void setAllConceptCount(long allConceptCount) {
    this.allConceptCount = allConceptCount;
  }

  public DomainInfo allConceptCount(long allConceptCount) {
    this.allConceptCount = allConceptCount;
    return this;
  }

  @Column(name="standard_concept_count")
  public long getStandardConceptCount() {
    return standardConceptCount;
  }

  public void setStandardConceptCount(long standardConceptCount) {
    this.standardConceptCount = standardConceptCount;
  }

  public DomainInfo standardConceptCount(long standardConceptCount) {
    this.standardConceptCount = standardConceptCount;
    return this;
  }

  @Column(name="participant_count")
  public long getParticipantCount(){
    return participantCount;
  }

  public void setParticipantCount(long participantCount){
    this.participantCount = participantCount;
  }

  public DomainInfo participantCount(long participantCount){
    this.participantCount = participantCount;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DomainInfo domainInfo = (DomainInfo) o;
    return Objects.equals(domain, domainInfo.domain) &&
        Objects.equals(name, domainInfo.name) &&
        Objects.equals(description, domainInfo.description) &&
        Objects.equals(conceptId, domainInfo.conceptId) &&
        Objects.equals(allConceptCount, domainInfo.allConceptCount) &&
        Objects.equals(standardConceptCount, domainInfo.standardConceptCount) &&
        Objects.equals(participantCount, domainInfo.participantCount);
  }

  @Override
  public int hashCode() {
    return Objects.hash(domain, name, description, conceptId, allConceptCount, standardConceptCount, participantCount);
  }

  @Override
  public String toString() {
    return  ToStringBuilder.reflectionToString(this);
  }

}
