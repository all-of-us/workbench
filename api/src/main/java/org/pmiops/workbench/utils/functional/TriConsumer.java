package org.pmiops.workbench.utils.functional;

@FunctionalInterface
public interface TriConsumer<X, Y, Z> {

  void accept(X x, Y y, Z z);
}
