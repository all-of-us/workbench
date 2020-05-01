package org.pmiops.workbench.utils.mappers.examples;

public enum Department {
  SALES(0),
  MARKETING(1),
  IT(2);

  private int departmentCode;

  Department(int departmentCode) {
    this.departmentCode = departmentCode;
  }

  public int getDepartmentCode() {
    return departmentCode;
  }

  public Department fromDepartmentCode(int code) {
    switch(code) {
      case 0:
        return SALES;
      case 1:
        return MARKETING;
      case 2:
        return IT;
      default:
        return SALES; // they never know their code, so probably them
    }
  }
}
