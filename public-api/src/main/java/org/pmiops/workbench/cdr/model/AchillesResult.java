package org.pmiops.workbench.cdr.model;

import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import org.pmiops.workbench.cdr.model.AchillesAnalysis;

@Entity
//TODO need to add a way to dynamically switch between database versions
//this dynamic connection will eliminate the need for the catalog attribute
@Table(name = "achilles_results")
public class AchillesResult  {

    private Long id;
    private Long analysisId;
    private AchillesAnalysis analysis;
    private String stratum1;
    private String stratum2;
    private String stratum3;
    private String stratum4;
    private String stratum5;
    private Long countValue;
    private String stratum5Name;

    @Id
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public AchillesResult id(Long val) {
        this.id = val;
        return this;
    }

    @Column(name="analysis_id")
    public Long getAnalysisId() {
        return analysisId;
    }
    public void setAnalysisId(Long analysisId) {
        this.analysisId = analysisId;
    }
    public AchillesResult analysisId(Long val) {
        this.analysisId = val;
        return this;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="analysis_id", insertable=false, updatable=false)
    public AchillesAnalysis getAnalysis() {
        return analysis;
    }
    public void setAnalysis(AchillesAnalysis analysis) {
        this.analysis = analysis;
    }
    public AchillesResult analysis(AchillesAnalysis analysis) {
        this.analysis = analysis;
        return this;
    }

    @Column(name="stratum_1")
    public String getStratum1() {
        return stratum1;
    }
    public void setStratum1(String stratum1) {
        this.stratum1 = stratum1;
    }
    public AchillesResult stratum1(String val) {
        this.stratum1 = val;
        return this;
    }

    @Column(name="stratum_2")
    public String getStratum2() {
        return stratum2;
    }
    public void setStratum2(String stratum2) {
        this.stratum2 = stratum2;
    }
    public AchillesResult stratum2(String val) {
        this.stratum2 = val;
        return this;
    }

    @Column(name="stratum_3")
    public String getStratum3() {
        return stratum3;
    }
    public void setStratum3(String stratum3) {
        this.stratum3 = stratum3;
    }
    public AchillesResult stratum3(String val) {
        this.stratum3 = val;
        return this;
    }

    @Column(name="stratum_4")
    public String getStratum4() {
        return stratum4;
    }
    public void setStratum4(String stratum4) {
        this.stratum4 = stratum4;
    }
    public AchillesResult stratum4(String val) {
        this.stratum4 = val;
        return this;
    }

    @Column(name="stratum_5")
    public String getStratum5() {
        return stratum5;
    }
    public void setStratum5(String stratum5) {
        this.stratum5 = stratum5;
    }
    public AchillesResult stratum5(String val) {
        this.stratum5 = val;
        return this;
    }

    @Transient
    public String getStratum5Name() {
        return stratum5Name;
    }
    public void setStratum5Name(String stratum5Name) {
        this.stratum5Name = stratum5Name;
    }
    public AchillesResult stratum5Name(String val) {
        this.stratum5Name = val;
        return this;
    }

    @Column(name="count_value")
    public Long getCountValue() {
        return countValue;
    }
    public void setCountValue(Long countValue) {
        this.countValue = countValue;
    }
    public AchillesResult countValue(Long val) {
        this.countValue = val;
        return this;
    }

    @Override
    public String toString() {
        return "AchillesResult{" +
                "id=" + id +
                ", analysisId=" + analysisId +
                ", analysis=" + analysis +
                ", stratum1='" + stratum1 + '\'' +
                ", stratum2='" + stratum2 + '\'' +
                ", stratum3='" + stratum3 + '\'' +
                ", stratum4='" + stratum4 + '\'' +
                ", stratum5='" + stratum5 + '\'' +
                ", countValue=" + countValue +
                ", stratum5Name='" + stratum5Name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AchillesResult that = (AchillesResult) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (analysisId != null ? !analysisId.equals(that.analysisId) : that.analysisId != null) return false;
        if (analysis != null ? !analysis.equals(that.analysis) : that.analysis != null) return false;
        if (stratum1 != null ? !stratum1.equals(that.stratum1) : that.stratum1 != null) return false;
        if (stratum2 != null ? !stratum2.equals(that.stratum2) : that.stratum2 != null) return false;
        if (stratum3 != null ? !stratum3.equals(that.stratum3) : that.stratum3 != null) return false;
        if (stratum4 != null ? !stratum4.equals(that.stratum4) : that.stratum4 != null) return false;
        if (stratum5 != null ? !stratum5.equals(that.stratum5) : that.stratum5 != null) return false;
        if (countValue != null ? !countValue.equals(that.countValue) : that.countValue != null) return false;
        return stratum5Name != null ? stratum5Name.equals(that.stratum5Name) : that.stratum5Name == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (analysisId != null ? analysisId.hashCode() : 0);
        result = 31 * result + (analysis != null ? analysis.hashCode() : 0);
        result = 31 * result + (stratum1 != null ? stratum1.hashCode() : 0);
        result = 31 * result + (stratum2 != null ? stratum2.hashCode() : 0);
        result = 31 * result + (stratum3 != null ? stratum3.hashCode() : 0);
        result = 31 * result + (stratum4 != null ? stratum4.hashCode() : 0);
        result = 31 * result + (stratum5 != null ? stratum5.hashCode() : 0);
        result = 31 * result + (countValue != null ? countValue.hashCode() : 0);
        result = 31 * result + (stratum5Name != null ? stratum5Name.hashCode() : 0);
        return result;
    }
}
