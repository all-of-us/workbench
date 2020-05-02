package org.pmiops.workbench.utils.mappers.examples;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper
public interface EmployeeMapper {

  @Mapping(source = "employeeDbEntity", target = "name", qualifiedByName = "toFullName")
  EmployeeModel toModel(EmployeeDbEntity employeeDbEntity);

  @Named("toFullName")
  default String toName(EmployeeDbEntity employeeDbEntity) {
    return String.format("%s %s", employeeDbEntity.getFirstName(), employeeDbEntity.getLastName());
  }
}
