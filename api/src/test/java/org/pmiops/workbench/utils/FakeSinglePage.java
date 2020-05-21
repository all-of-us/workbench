package org.pmiops.workbench.utils;

import com.google.api.gax.paging.Page;

public class FakeSinglePage<T> implements Page<T> {

  public static final String NO_NEXT_PAGE_TOKEN = "";
  private Iterable<T> iterable;

  public FakeSinglePage(Iterable<T> iterable) {

    this.iterable = iterable;
  }

  @Override
  public boolean hasNextPage() {
    return false;
  }

  @Override
  public String getNextPageToken() {
    return NO_NEXT_PAGE_TOKEN;
  }

  @Override
  public Page<T> getNextPage() {
    return null;
  }

  @Override
  public Iterable<T> iterateAll() {
    return iterable;
  }

  @Override
  public Iterable<T> getValues() {
    return iterable;
  }
}
