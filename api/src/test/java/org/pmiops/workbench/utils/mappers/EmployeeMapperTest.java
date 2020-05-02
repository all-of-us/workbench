package org.pmiops.workbench.utils.mappers;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.utils.mappers.examples.Department;
import org.pmiops.workbench.utils.mappers.examples.EmployeeDbEntity;
import org.pmiops.workbench.utils.mappers.examples.EmployeeMapper;
import org.pmiops.workbench.utils.mappers.examples.EmployeeMapperImpl;
import org.pmiops.workbench.utils.mappers.examples.EmployeeModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class EmployeeMapperTest {
  @Autowired
  private EmployeeMapper employeeMapper;

  @TestConfiguration
  @Import({
      EmployeeMapperImpl.class
  }) static class Configuration {

  }

  @Test
  testToModel() {
    final EmployeeDbEntity employeeDbEntity = new EmployeeDbEntity("John", "Doe", 2, "3.00",
        address);

    final EmployeeModel employeeModel = employeeMapper.toModel(employeeDbEntity);
    assertThat(employeeModel.getDepartment()).isEqualTo(Department.MARKETING);
    assertThat(employeeModel.getSalary() - 6000.0).isWithin(0.50);
  }
}
