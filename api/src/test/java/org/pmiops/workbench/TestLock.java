package org.pmiops.workbench;

public class TestLock {

  private boolean locked = false;

  public int lock() {
    synchronized (this) {
      if (locked) {
        return 0;
      }

      locked = true;
      return 1;
    }
  }

  synchronized public int release() {
    synchronized (this) {
      if (locked) {
        locked = false;
        return 1;
      }

      return 0;
    }
  }

}
