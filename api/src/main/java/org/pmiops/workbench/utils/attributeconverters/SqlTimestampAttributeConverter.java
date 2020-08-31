package org.pmiops.workbench.utils.attributeconverters;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import org.pmiops.workbench.utils.mappers.TimeMappers;

@Converter(autoApply = true)
public class SqlTimestampAttributeConverter
    implements AttributeConverter<Timestamp, OffsetDateTime> {

  @Override
  public OffsetDateTime convertToDatabaseColumn(Timestamp attribute) {
    return TimeMappers.offsetDateTime(attribute);
  }

  @Override
  public Timestamp convertToEntityAttribute(OffsetDateTime dbData) {
    return TimeMappers.timestamp(dbData);
  }
}
