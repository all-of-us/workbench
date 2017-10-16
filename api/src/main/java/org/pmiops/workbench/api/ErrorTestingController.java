package org.pmiops.workbench.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;



@RestController
public class ErrorTestingController implements ErrorTestingApiDelegate {

  @Override
  public ResponseEntity<Void> failFourHundred() {
    return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
  }

  @Override
  public ResponseEntity<Void> failFiveHundred() {
    throw new RuntimeException("Expected Server Error");
  }

  @Override
  public ResponseEntity<Void> failFiveHundredAndThree() {
    return new ResponseEntity<Void>(HttpStatus.SERVICE_UNAVAILABLE);
  }

}
