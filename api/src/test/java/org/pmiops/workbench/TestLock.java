package org.pmiops.workbench;

public class TestLock {
  
  private boolean locked = false;

  public int lock() {
    if (locked) {
      return 0;
    }

    locked = true;
    return 1;
  }

  public int release() {
    if (locked) {
      locked = false;
      return 1;
    }

    return 0;
  }
}
