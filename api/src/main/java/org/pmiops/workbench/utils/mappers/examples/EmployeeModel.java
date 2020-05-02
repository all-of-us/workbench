package org.pmiops.workbench.utils.mappers.examples;

public class EmployeeModel {
  private String name;
  private Department department;
  private double salary;

  public EmployeeModel() {
  }

  public EmployeeModel(String name,
      Department department, double salary) {
    this.name = name;
    this.department = department;
    this.salary = salary;
  }

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
  public void setSalary(double salary) {
    this.salary = salary;
  }

  public double getWeeklySalary() {
    return getSalary() / 52.0;
  }
}
