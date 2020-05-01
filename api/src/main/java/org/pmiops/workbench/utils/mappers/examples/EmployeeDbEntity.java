package org.pmiops.workbench.utils.mappers.examples;

public class EmployeeDbEntity {
  String firstName;

  String lastName;
  int departmentCode;

  public EmployeeDbEntity(String firstName, String lastName, int departmentCode, double hourlyRate) {
    this.firstName = firstName;
    this.lastName = lastName;
    this.departmentCode = departmentCode;
    this.hourlyRate = hourlyRate;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public int getDepartmentCode() {
    return departmentCode;
  }

  public void setDepartmentCode(int departmentCode) {
    this.departmentCode = departmentCode;
  }

  public double getHourlyRate() {
    return hourlyRate;
  }

  public void setHourlyRate(double hourlyRate) {
    this.hourlyRate = hourlyRate;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }


  double hourlyRate;
}
