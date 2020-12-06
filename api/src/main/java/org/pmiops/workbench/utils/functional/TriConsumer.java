package org.pmiops.workbench.utils.functional;

import java.util.Objects;
import java.util.function.Function;

@FunctionalInterface
public interface TriConsumer<X, Y, Z> {

  void accept(X x, Y y, Z z);
}
