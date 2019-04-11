package org.pmiops.workbench.db.model;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Table;

@Embeddable
@Table(name = "data_set_cohort_id")
public class DataSetValues {

  private String domainId;
  private String values;

  public DataSetValues() {
  }

  public DataSetValues(String domainId, String values) {
    this.domainId = domainId;
    this.values = values;
  }

  @Column(name = "domain_id")
  public String getDomainId() {
    return domainId;
  }

  public void setDomainId(String domainId) {
    this.domainId = domainId;
  }


  @Column(name = "values")
  public String getValues() {
    return values;
  }

  public void setValues(String values) {
    this.values = values;
  }
}
