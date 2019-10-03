package org.pmiops.workbench.db.model;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.pmiops.workbench.model.Domain;

@Embeddable
@Table(name = "data_set_cohort_id")
public class DataSetValues {

  private String domainId;
  private String value;

  public DataSetValues() {}

  public DataSetValues(String domainId, String value) {
    this.domainId = domainId;
    this.value = value;
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
    return CommonStorageEnums.domainFromStorage(Short.parseShort(domainId));
  }

  @Column(name = "value")
  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }
}
