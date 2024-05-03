package org.pmiops.workbench.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.sql.Timestamp;
import org.pmiops.workbench.model.RdrEntity;

@Entity
@Table(name = "rdr_export")
public class DbRdrExport {
  private int exportId;
  private Short entityType;
  private Long entityId;
  private Timestamp lastExportDate;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "export_id")
  public int getExportId() {
    return exportId;
  }

  public DbRdrExport setExportId(int exportId) {
    this.exportId = exportId;
    return this;
  }

  @Column(name = "entity_type")
  public Short getEntityType() {
    return entityType;
  }

  public DbRdrExport setEntityType(Short entityType) {
    this.entityType = entityType;
    return this;
  }

  @Transient
  public RdrEntity getEntityTypeEnum() {
    if (entityType == null) return null;
    return RdrEntityEnums.entityFromStorage(entityType);
  }

  public DbRdrExport setEntityTypeEnum(RdrEntity entityTypeEnum) {
    this.entityType = RdrEntityEnums.entityToStorage(entityTypeEnum);
    return this;
  }

  @Column(name = "entity_id")
  public long getEntityId() {
    return entityId;
  }

  public DbRdrExport setEntityId(long id) {
    this.entityId = id;
    return this;
  }

  @Column(name = "last_export_date")
  public Timestamp getLastExportDate() {
    return lastExportDate;
  }

  public DbRdrExport setLastExportDate(Timestamp exportDate) {
    this.lastExportDate = exportDate;
    return this;
  }
}
