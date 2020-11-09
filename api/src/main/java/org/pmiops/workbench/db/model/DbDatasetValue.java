package org.pmiops.workbench.db.model;

import java.util.Objects;
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

  public DbDatasetValue(DbDatasetValue datasetValue) {
    setDomainId(datasetValue.getDomainId());
    setValue(datasetValue.getValue());
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

  public boolean equals(DbDatasetValue datasetValue) {
    return getValue().equals(datasetValue.getValue())
        && getDomainEnum().equals(datasetValue.getDomainEnum());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DbDatasetValue that = (DbDatasetValue) o;
    return Objects.equals(domainId, that.domainId) && Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(domainId, value);
  }
}
