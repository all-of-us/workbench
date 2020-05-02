package org.pmiops.workbench.utils.mappers.examples;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface EmployeeMapper {

  @Mapping(source = "employeeDbEntity", target = "name")
  EmployeeModel toModel(EmployeeDbEntity employeeDbEntity);

  default String toNickname(EmployeeDbEntity employeeDbEntity) {
    return String.format("%s %s", employeeDbEntity.getFirstName(), employeeDbEntity.getLastName());
  }
}
