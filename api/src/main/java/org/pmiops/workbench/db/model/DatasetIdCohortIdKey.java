package org.pmiops.workbench.db.model;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class DatasetIdCohortIdKey implements Serializable {
  private Long datasetId;
  private Long cohortId;

  public DatasetIdCohortIdKey() {
  }

  @Column(name = "data_set_id")
  public Long getDatasetId() {
    return datasetId;
  }

  public void setDatasetId(Long datasetId) {
    this.datasetId = datasetId;
  }

  @Column(name = "cohort_id")
  public Long getCohortId() {
    return cohortId;
  }

  public void setCohortId(Long cohortId) {
    this.cohortId = cohortId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DatasetIdCohortIdKey)) {
      return false;
    }
    DatasetIdCohortIdKey that = (DatasetIdCohortIdKey) o;
    return Objects.equals(getDatasetId(), that.getDatasetId()) &&
        Objects.equals(getCohortId(), that.getCohortId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getDatasetId(), getCohortId());
  }
}
