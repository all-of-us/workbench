package org.pmiops.workbench.utils;

import java.util.logging.Level;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import org.springframework.http.ResponseEntity;

public class EmailAddressUtils {
  public static boolean isValidAddress(String address) {
    try {
      new InternetAddress(address).validate();
      return true;
    } catch (AddressException e) {
      return false;
    }
  }
}
