package org.pmiops.workbench.utils.mappers.examples;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(uses = SampleEnumMapper.class)
public interface EmployeeMapper {

  double HOURS_IN_YEAR = 2000.0;

  @Mapping(source = "employeeDbEntity", target = "name")
  @Mapping(source = "employeeDbEntity.departmentCode", target = "department")
  @Mapping(source = "employeeDbEntity.hourlyRate", target = "salary")
  EmployeeModel toModel(EmployeeDbEntity employeeDbEntity);

  default String toNickname(EmployeeDbEntity employeeDbEntity) {
    return String.format("%s %s", employeeDbEntity.getFirstName(), employeeDbEntity.getLastName());
  }

  default double toAnnalSalary(double hourlyRate) {
    return hourlyRate * HOURS_IN_YEAR;
  }
}
