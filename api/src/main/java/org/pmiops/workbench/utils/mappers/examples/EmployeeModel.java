package org.pmiops.workbench.utils.mappers.examples;

/**
 * Demo API model class
 */
public class EmployeeModel {

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Department getDepartment() {
    return department;
  }

  public void setDepartment(Department department) {
    this.department = department;
  }

  public double getSalary() {
    return salary;
  }

  public double getWeeklySalary() {
    return getSalary() / 52.0;
  }

  public void setSalary(double salary) {
    this.salary = salary;
  }

  String name;
  Department department;

  public EmployeeModel(String name,
      Department department, double salary) {
    this.name = name;
    this.department = department;
    this.salary = salary;
  }

  double salary;

}
