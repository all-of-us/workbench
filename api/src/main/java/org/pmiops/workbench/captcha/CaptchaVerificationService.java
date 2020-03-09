package org.pmiops.workbench.captcha;

public interface CaptchaVerificationService {

  boolean verifyCaptcha(String responseToken) throws ApiException;
}
