package org.pmiops.workbench.exceptions;

import org.springframework.http.HttpStatus;

public interface DefinesHttpResponseCode {
  HttpStatus statusCode();
}
