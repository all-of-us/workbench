package org.pmiops.workbench.db.model;

import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import main.java.org.pmiops.workbench.db.model.RDREntityEnums;
import org.pmiops.workbench.model.RDREntity;

@Entity
@Table(name = "rdr_export")
public class DbRDRExport {
  private int exportId;
  private Short entity;
  private int id;
  private Timestamp exportDate;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "export_id")
  public int getExportId() {
    return exportId;
  }

  public void setExportId(int exportId) {
    this.exportId = exportId;
  }

  @Column(name = "entity")
  public Short getEntity() {
    return entity;
  }

  public void setEntity(Short entity) {
    this.entity = entity;
  }

  @Transient
  public RDREntity getEntityEnum() {
    if (entity == null) return null;
    return RDREntityEnums.entityFromStorage(entity);
  }

  public void setEntityEnum(RDREntity entityEnum) {
    this.entity = RDREntityEnums.entityToStorage(entityEnum);
  }

  @Column(name = "entity_id")
  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  @Column(name = "last_export_date")
  public Timestamp getExportDate() {
    return exportDate;
  }

  public void setExportDate(Timestamp exportDate) {
    this.exportDate = exportDate;
  }
}
