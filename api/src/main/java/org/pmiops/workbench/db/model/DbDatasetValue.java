package org.pmiops.workbench.db.model;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.pmiops.workbench.model.Domain;

@Embeddable
@Table(name = "data_set_cohort_id")
public class DbDatasetValue {

  private String domainId;
  private String value;

  public DbDatasetValue() {}

  public DbDatasetValue(String domainId, String value) {
    this.domainId = domainId;
    this.value = value;
  }

  public DbDatasetValue(DbDatasetValue dataSetValue) {
    setDomainId(dataSetValue.getDomainId());
    setValue(dataSetValue.getValue());
  }

  @Column(name = "domain_id")
  public String getDomainId() {
    return domainId;
  }

  public void setDomainId(String domainId) {
    this.domainId = domainId;
  }

  @Transient
  public Domain getDomainEnum() {
    return DbStorageEnums.domainFromStorage(Short.parseShort(domainId));
  }

  @Column(name = "value")
  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public boolean equals(DbDatasetValue dataSetValue) {
    return getValue().equals(dataSetValue.getValue())
        && getDomainEnum().equals(dataSetValue.getDomainEnum());
  }
}
