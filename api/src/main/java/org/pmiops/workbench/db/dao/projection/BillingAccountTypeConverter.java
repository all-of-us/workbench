package org.pmiops.workbench.db.dao.projection;

import javax.persistence.AttributeConverter;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.model.BillingAccountType;

// @Converter(autoApply = true)
public class BillingAccountTypeConverter implements AttributeConverter<Short, BillingAccountType> {

  @Override
  public BillingAccountType convertToDatabaseColumn(Short attribute) {
    return DbStorageEnums.billingAccountTypeFromStorage(attribute);
  }

  @Override
  public Short convertToEntityAttribute(BillingAccountType dbData) {
    return DbStorageEnums.billingAccountTypeToStorage(dbData);
  }
}
