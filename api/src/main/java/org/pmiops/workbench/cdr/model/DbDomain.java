package org.pmiops.workbench.cdr.model;

import javax.persistence.*;


@Entity
//TODO need to add a way to dynamically switch between database versions
//this dynamic connection will eliminate the need for the catalog attribute
@Table(name = "db_domain", catalog="cdr")
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

        if (domainId != null ? !domainId.equals(dbDomain.domainId) : dbDomain.domainId != null) return false;
        if (domainDisplay != null ? !domainDisplay.equals(dbDomain.domainDisplay) : dbDomain.domainDisplay != null)
            return false;
        if (domainDesc != null ? !domainDesc.equals(dbDomain.domainDesc) : dbDomain.domainDesc != null) return false;
        if (domainParent != null ? !domainParent.equals(dbDomain.domainParent) : dbDomain.domainParent != null)
            return false;
        return domainRoute != null ? domainRoute.equals(dbDomain.domainRoute) : dbDomain.domainRoute == null;
    }

    @Override
    public int hashCode() {
        int result = domainId != null ? domainId.hashCode() : 0;
        result = 31 * result + (domainDisplay != null ? domainDisplay.hashCode() : 0);
        result = 31 * result + (domainDesc != null ? domainDesc.hashCode() : 0);
        result = 31 * result + (domainParent != null ? domainParent.hashCode() : 0);
        result = 31 * result + (domainRoute != null ? domainRoute.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DbDomain{" +
                "domainId='" + domainId + '\'' +
                ", domainDisplay='" + domainDisplay + '\'' +
                ", domainDesc='" + domainDesc + '\'' +
                ", domainParent='" + domainParent + '\'' +
                ", domainRoute='" + domainRoute + '\'' +
                '}';
    }
}
