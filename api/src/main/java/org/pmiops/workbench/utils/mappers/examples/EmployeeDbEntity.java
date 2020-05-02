package org.pmiops.workbench.utils.mappers.examples;

// ORM details omitted.
public class EmployeeDbEntity {

  private String firstName;
  private String lastName;
  private int departmentCode; // corresponds to enum
  private double hourlyRate;
  private String address;

  public EmployeeDbEntity(String firstName, String lastName, int departmentCode, double hourlyRate,
      String address) {
    this.firstName = firstName;
    this.lastName = lastName;
    this.departmentCode = departmentCode;
    this.hourlyRate = hourlyRate;
    this.address = address;
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

  public String getAddress() {
    return address;
  }
  public void setAddress(String address) {
    this.address = address;
  }
}
