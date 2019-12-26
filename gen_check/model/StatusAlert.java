package org.pmiops.workbench.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.Valid;
import javax.validation.constraints.*;

/**
 * StatusAlert
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T14:53:13.438-06:00")

public class StatusAlert   {
  @JsonProperty("statusAlertId")
  private Long statusAlertId = null;

  @JsonProperty("title")
  private String title = null;

  @JsonProperty("message")
  private String message = null;

  @JsonProperty("link")
  private String link = null;

  public StatusAlert statusAlertId(Long statusAlertId) {
    this.statusAlertId = statusAlertId;
    return this;
  }

   /**
   * The primary key of the status alert in the database
   * @return statusAlertId
  **/
  @ApiModelProperty(value = "The primary key of the status alert in the database")


  public Long getStatusAlertId() {
    return statusAlertId;
  }

  public void setStatusAlertId(Long statusAlertId) {
    this.statusAlertId = statusAlertId;
  }

  public StatusAlert title(String title) {
    this.title = title;
    return this;
  }

   /**
   * Title of the status alert, e.g. 'Service Incident: December 6, 2019' 
   * @return title
  **/
  @ApiModelProperty(value = "Title of the status alert, e.g. 'Service Incident: December 6, 2019' ")


  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public StatusAlert message(String message) {
    this.message = message;
    return this;
  }

   /**
   * The actual message of the status alert, e.g. 'AoU RW is down because GCP is down' 
   * @return message
  **/
  @ApiModelProperty(value = "The actual message of the status alert, e.g. 'AoU RW is down because GCP is down' ")


  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public StatusAlert link(String link) {
    this.link = link;
    return this;
  }

   /**
   * A URL linking to an incident report where users can read more 
   * @return link
  **/
  @ApiModelProperty(value = "A URL linking to an incident report where users can read more ")


  public String getLink() {
    return link;
  }

  public void setLink(String link) {
    this.link = link;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StatusAlert statusAlert = (StatusAlert) o;
    return Objects.equals(this.statusAlertId, statusAlert.statusAlertId) &&
        Objects.equals(this.title, statusAlert.title) &&
        Objects.equals(this.message, statusAlert.message) &&
        Objects.equals(this.link, statusAlert.link);
  }

  @Override
  public int hashCode() {
    return Objects.hash(statusAlertId, title, message, link);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StatusAlert {\n");
    
    sb.append("    statusAlertId: ").append(toIndentedString(statusAlertId)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
    sb.append("    link: ").append(toIndentedString(link)).append("\n");
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

