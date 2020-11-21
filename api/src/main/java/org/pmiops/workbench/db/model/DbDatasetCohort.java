package org.pmiops.workbench.db.model;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import jdk.nashorn.internal.objects.annotations.Getter;
import jdk.nashorn.internal.objects.annotations.Setter;

/**
 * Join table to describe cohorts used in datasets, which is a
 * @ManyToMany relation.
 */
@Entity
@Table(name = "data_set_cohort")
public class DbDatasetCohort {

  private DatasetIdCohortIdKey id;
  private DbDataset dataset;
  private DbCohort cohort;
;
  public DbDatasetCohort() {
  }

  @EmbeddedId
  public DatasetIdCohortIdKey getId() {
    return id;
  }
  public void setId(DatasetIdCohortIdKey id) {
    this.id = id;
  }

//  @Column(name = "data_set_id")
  @OneToOne(targetEntity = DbDataset.class, mappedBy = "datasetId", fetch = FetchType.EAGER)
//  @MapsId(value = "data_set_id")
//  @JoinColumn(name = "data_set_id")
  public DbDataset getDataset() {
    return dataset;
  }

  public void setDataset(DbDataset dataset) {
    this.dataset = dataset;
  }

//  @Column(name = "cohort_id")
  @OneToOne(targetEntity = DbCohort.class, mappedBy = "cohortId", fetch = FetchType.EAGER)
//  @JoinColumn(name = "cohort_id")
  public DbCohort getCohort() {
    return cohort;
  }

  public void setCohort(DbCohort cohort) {
    this.cohort = cohort;
  }


//  public Long getCohortId() {
//    return cohortId;
//  }
//
//  public void setCohortId(Long cohortId) {
//    this.cohortId = cohortId;
//  }
//
//  public Long getDatasetId() {
//    return datasetId;
//  }
//
//  public void setDatasetId(Long datasetId) {
//    this.datasetId = datasetId;
//  }
}
