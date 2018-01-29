package org.pmiops.workbench.cdr.model;

import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.persistence.*;
import java.util.Objects;


@Entity
//TODO need to add a way to dynamically switch between database versions
//this dynamic connection will eliminate the need for the catalog attribute
@Table(name = "db_domain")
public class DbDomain {

    private String domainId;
    private String domainDisplay;
    private String domainDesc;
    private String domainParent;
    private String domainRoute;


    @Id
    @Column(name = "domain_id")
    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public DbDomain domainId(String domainId) {
        this.domainId = domainId;
        return this;
    }

    @Column(name = "domain_display")
    public String getDomainDisplay() {
        return domainDisplay;
    }

    public void setDomainDisplay(String domainDisplay) {
        this.domainDisplay = domainDisplay;
    }

    public DbDomain DomainDisplay(String domainDisplay) {
        this.domainDisplay = domainDisplay;
        return this;
    }

    @Column(name = "domain_desc")
    public String getDomainDesc() {
        return domainDesc;
    }

    public void setDomainDesc(String domainDesc) {
        this.domainDesc = domainDesc;
    }

    public DbDomain domainDesc(String domainDesc) {
        this.domainDesc = domainDesc;
        return this;
    }

    @Column(name = "domain_parent")
    public String getDomainParent() {
        return domainParent;
    }

    public void setDomainParent(String domainParent) {
        this.domainParent = domainParent;
    }

    public DbDomain domainParent(String domainParent) {
        this.domainParent = domainParent;
        return this;
    }

    @Column(name = "domain_route")
    public String getDomainRoute() {
        return domainRoute;
    }

    public void setDomainRoute(String domainRoute) {
        this.domainRoute = domainRoute;
    }

    public DbDomain domainRoute(String domainRoute) {
        this.domainRoute = domainRoute;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DbDomain dbDomain = (DbDomain) o;
        return Objects.equals(domainId, dbDomain.domainId) &&
                Objects.equals(domainDisplay, dbDomain.domainDisplay) &&
                Objects.equals(domainDesc, dbDomain.domainDesc) &&
                Objects.equals(domainParent, dbDomain.domainParent) &&
                Objects.equals(domainRoute, dbDomain.domainRoute);
    }

    @Override
    public int hashCode() {
        return Objects.hash(domainId, domainDisplay, domainDesc, domainParent, domainRoute);
    }

    @Override
    public String toString() {
        return  ToStringBuilder.reflectionToString(this);

    }



}
