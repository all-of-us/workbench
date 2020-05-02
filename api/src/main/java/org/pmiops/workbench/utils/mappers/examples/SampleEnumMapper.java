package org.pmiops.workbench.utils.mappers.examples;

import org.mapstruct.Mapper;

@Mapper
public interface SampleEnumMapper {
  default Department toDepartment(int departmentCode) {
    return Department.fromDepartmentCode(departmentCode);
  }
}
