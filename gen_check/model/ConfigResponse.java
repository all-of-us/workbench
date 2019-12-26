package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * ConfigResponse
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T14:53:13.438-06:00")

public class ConfigResponse   {
  @JsonProperty("gsuiteDomain")
  private String gsuiteDomain = null;

  @JsonProperty("projectId")
  private String projectId = null;

  @JsonProperty("publicApiKeyForErrorReports")
  private String publicApiKeyForErrorReports = null;

  @JsonProperty("enableComplianceTraining")
  private Boolean enableComplianceTraining = false;

  @JsonProperty("enableDataUseAgreement")
  private Boolean enableDataUseAgreement = false;

  @JsonProperty("unsafeAllowSelfBypass")
  private Boolean unsafeAllowSelfBypass = false;

  @JsonProperty("enableEraCommons")
  private Boolean enableEraCommons = false;

  @JsonProperty("firecloudURL")
  private String firecloudURL = null;

  public ConfigResponse gsuiteDomain(String gsuiteDomain) {
    this.gsuiteDomain = gsuiteDomain;
    return this;
  }

   /**
   * G-Suite domain containing user accounts.
   * @return gsuiteDomain
  **/
  @ApiModelProperty(required = true, value = "G-Suite domain containing user accounts.")
  @NotNull


  public String getGsuiteDomain() {
    return gsuiteDomain;
  }

  public void setGsuiteDomain(String gsuiteDomain) {
    this.gsuiteDomain = gsuiteDomain;
  }

  public ConfigResponse projectId(String projectId) {
    this.projectId = projectId;
    return this;
  }

   /**
   * The cloud project in which this app is running.
   * @return projectId
  **/
  @ApiModelProperty(value = "The cloud project in which this app is running.")


  public String getProjectId() {
    return projectId;
  }

  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }

  public ConfigResponse publicApiKeyForErrorReports(String publicApiKeyForErrorReports) {
    this.publicApiKeyForErrorReports = publicApiKeyForErrorReports;
    return this;
  }

   /**
   * Stackdriver API key for error reporting, scoped to a particular domain. If unset, Stackdriver error reporting should be disabled. 
   * @return publicApiKeyForErrorReports
  **/
  @ApiModelProperty(value = "Stackdriver API key for error reporting, scoped to a particular domain. If unset, Stackdriver error reporting should be disabled. ")


  public String getPublicApiKeyForErrorReports() {
    return publicApiKeyForErrorReports;
  }

  public void setPublicApiKeyForErrorReports(String publicApiKeyForErrorReports) {
    this.publicApiKeyForErrorReports = publicApiKeyForErrorReports;
  }

  public ConfigResponse enableComplianceTraining(Boolean enableComplianceTraining) {
    this.enableComplianceTraining = enableComplianceTraining;
    return this;
  }

   /**
   * Feature flag for enabling compliance training in registration steps
   * @return enableComplianceTraining
  **/
  @ApiModelProperty(value = "Feature flag for enabling compliance training in registration steps")


  public Boolean getEnableComplianceTraining() {
    return enableComplianceTraining;
  }

  public void setEnableComplianceTraining(Boolean enableComplianceTraining) {
    this.enableComplianceTraining = enableComplianceTraining;
  }

  public ConfigResponse enableDataUseAgreement(Boolean enableDataUseAgreement) {
    this.enableDataUseAgreement = enableDataUseAgreement;
    return this;
  }

   /**
   * Feature flag for enabling data use agreement
   * @return enableDataUseAgreement
  **/
  @ApiModelProperty(value = "Feature flag for enabling data use agreement")


  public Boolean getEnableDataUseAgreement() {
    return enableDataUseAgreement;
  }

  public void setEnableDataUseAgreement(Boolean enableDataUseAgreement) {
    this.enableDataUseAgreement = enableDataUseAgreement;
  }

  public ConfigResponse unsafeAllowSelfBypass(Boolean unsafeAllowSelfBypass) {
    this.unsafeAllowSelfBypass = unsafeAllowSelfBypass;
    return this;
  }

   /**
   * Enable a user to bypass themself
   * @return unsafeAllowSelfBypass
  **/
  @ApiModelProperty(value = "Enable a user to bypass themself")


  public Boolean getUnsafeAllowSelfBypass() {
    return unsafeAllowSelfBypass;
  }

  public void setUnsafeAllowSelfBypass(Boolean unsafeAllowSelfBypass) {
    this.unsafeAllowSelfBypass = unsafeAllowSelfBypass;
  }

  public ConfigResponse enableEraCommons(Boolean enableEraCommons) {
    this.enableEraCommons = enableEraCommons;
    return this;
  }

   /**
   * Feature flag for enabling eRA commons
   * @return enableEraCommons
  **/
  @ApiModelProperty(value = "Feature flag for enabling eRA commons")


  public Boolean getEnableEraCommons() {
    return enableEraCommons;
  }

  public void setEnableEraCommons(Boolean enableEraCommons) {
    this.enableEraCommons = enableEraCommons;
  }

  public ConfigResponse firecloudURL(String firecloudURL) {
    this.firecloudURL = firecloudURL;
    return this;
  }

   /**
   * The Firecloud URL to use for REST requests.
   * @return firecloudURL
  **/
  @ApiModelProperty(value = "The Firecloud URL to use for REST requests.")


  public String getFirecloudURL() {
    return firecloudURL;
  }

  public void setFirecloudURL(String firecloudURL) {
    this.firecloudURL = firecloudURL;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConfigResponse configResponse = (ConfigResponse) o;
    return Objects.equals(this.gsuiteDomain, configResponse.gsuiteDomain) &&
        Objects.equals(this.projectId, configResponse.projectId) &&
        Objects.equals(this.publicApiKeyForErrorReports, configResponse.publicApiKeyForErrorReports) &&
        Objects.equals(this.enableComplianceTraining, configResponse.enableComplianceTraining) &&
        Objects.equals(this.enableDataUseAgreement, configResponse.enableDataUseAgreement) &&
        Objects.equals(this.unsafeAllowSelfBypass, configResponse.unsafeAllowSelfBypass) &&
        Objects.equals(this.enableEraCommons, configResponse.enableEraCommons) &&
        Objects.equals(this.firecloudURL, configResponse.firecloudURL);
  }

  @Override
  public int hashCode() {
    return Objects.hash(gsuiteDomain, projectId, publicApiKeyForErrorReports, enableComplianceTraining, enableDataUseAgreement, unsafeAllowSelfBypass, enableEraCommons, firecloudURL);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConfigResponse {\n");
    
    sb.append("    gsuiteDomain: ").append(toIndentedString(gsuiteDomain)).append("\n");
    sb.append("    projectId: ").append(toIndentedString(projectId)).append("\n");
    sb.append("    publicApiKeyForErrorReports: ").append(toIndentedString(publicApiKeyForErrorReports)).append("\n");
    sb.append("    enableComplianceTraining: ").append(toIndentedString(enableComplianceTraining)).append("\n");
    sb.append("    enableDataUseAgreement: ").append(toIndentedString(enableDataUseAgreement)).append("\n");
    sb.append("    unsafeAllowSelfBypass: ").append(toIndentedString(unsafeAllowSelfBypass)).append("\n");
    sb.append("    enableEraCommons: ").append(toIndentedString(enableEraCommons)).append("\n");
    sb.append("    firecloudURL: ").append(toIndentedString(firecloudURL)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

