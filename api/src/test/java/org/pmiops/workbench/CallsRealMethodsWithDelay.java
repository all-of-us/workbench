package org.pmiops.workbench;

import org.mockito.internal.stubbing.answers.CallsRealMethods;
import org.mockito.invocation.InvocationOnMock;

public class CallsRealMethodsWithDelay extends CallsRealMethods {

  private final long delay;

  public CallsRealMethodsWithDelay(long delay) {
    this.delay = delay;
  }

  public Object answer(InvocationOnMock invocation) throws Throwable {
    Thread.sleep(delay);
    return super.answer(invocation);
  }

}