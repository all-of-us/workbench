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
    private String dbType;
    private String domainRoute;
    private long conceptId;
    private long countValue;


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

    public DbDomain domainDisplay(String domainDisplay) {
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

    @Column(name = "db_type")
    public String getDbType() {
        return dbType;
    }

    public void setDbType(String dbType) {
        this.dbType = dbType;
    }

    public DbDomain dbType(String dbType) {
        this.dbType = dbType;
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

    @Column(name = "concept_id",nullable = true)
    public Long getConceptId() {
        return conceptId;
    }

    public void setConceptId(Long conceptId) {
        this.conceptId = conceptId;
    }

    public DbDomain conceptId(Long conceptId) {
        this.conceptId = conceptId;
        return this;
    }

    @Column(name = "count_value")
    public Long getCountValue() {
        return countValue;
    }

    public void setCountValue(Long countValue) {
        this.countValue = countValue;
    }

    public DbDomain countValue(Long countValue) {
        this.countValue = countValue;
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
                Objects.equals(dbType, dbDomain.dbType) &&
                Objects.equals(domainRoute, dbDomain.domainRoute) &&
                Objects.equals(conceptId,dbDomain.conceptId) &&
                Objects.equals(countValue,dbDomain.countValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(domainId, domainDisplay, domainDesc, dbType, domainRoute, conceptId, countValue);
    }

    @Override
    public String toString() {
        return  ToStringBuilder.reflectionToString(this);

    }



}
