package org.pmiops.workbench.cdr.model;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Entity
@Table(name = "domain_vocabulary_info")
public class DomainVocabularyInfo {

  @Embeddable
  public static class DomainVocabularyInfoId implements Serializable {

    private String domainId;
    private String vocabularyId;

    public DomainVocabularyInfoId() {
    }

    public DomainVocabularyInfoId(String domainId, String vocabularyId) {
      this.domainId = domainId;
      this.vocabularyId = vocabularyId;
    }

    public String getDomainId() {
      return domainId;
    }

    public void setDomainId(String domainId) {
      this.domainId = domainId;
    }

    public DomainVocabularyInfoId domainId(String domainId) {
      this.domainId = domainId;
      return this;
    }

    public String getVocabularyId() {
      return vocabularyId;
    }

    public void setVocabularyId(String vocabularyId) {
      this.vocabularyId = vocabularyId;
    }

    public DomainVocabularyInfoId vocabularyId(String vocabularyId) {
      this.vocabularyId = vocabularyId;
      return this;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof DomainVocabularyInfoId)) return false;
      DomainVocabularyInfoId that = (DomainVocabularyInfoId) obj;
      return this.domainId.equals(that.domainId) && this.vocabularyId.equals(that.vocabularyId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.domainId, this.vocabularyId);
    }

    @Override
    public String toString() {
      return ToStringBuilder.reflectionToString(this);
    }

  }
  private DomainVocabularyInfoId id;
  private long allConceptCount;
  private long standardConceptCount;


  @EmbeddedId
  @AttributeOverrides({
      @AttributeOverride(name="domainId", column=@Column(name="domain_id")),
      @AttributeOverride(name="vocabularyId", column=@Column(name="vocabulary_id"))})
  public DomainVocabularyInfoId getId() {
    return id;
  }

  public void setId(DomainVocabularyInfoId id) {
    this.id = id;
  }

  public DomainVocabularyInfo id(DomainVocabularyInfoId id) {
    setId(id);
    return this;
  }

  @Column(name = "all_concept_count")
  public long getAllConceptCount() {
    return allConceptCount;
  }

  public void setAllConceptCount(long allConceptCount) {
    this.allConceptCount = allConceptCount;
  }

  public DomainVocabularyInfo allConceptCount(long allConceptCount) {
    setAllConceptCount(allConceptCount);
    return this;
  }

  @Column(name = "standard_concept_count")
  public long getStandardConceptCount() {
    return standardConceptCount;
  }

  public void setStandardConceptCount(long standardConceptCount) {
    this.standardConceptCount = standardConceptCount;
  }

  public DomainVocabularyInfo standardConceptCount(long allConceptCount) {
    setStandardConceptCount(allConceptCount);
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (!(obj instanceof DomainVocabularyInfo)) return false;
    DomainVocabularyInfo that = (DomainVocabularyInfo) obj;
    return this.id.equals(that.id) && this.allConceptCount == that.allConceptCount &&
        this.standardConceptCount == that.standardConceptCount;
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.id, this.allConceptCount, this.standardConceptCount);
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }
}
