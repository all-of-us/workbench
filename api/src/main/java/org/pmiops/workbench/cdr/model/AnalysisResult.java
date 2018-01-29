package org.pmiops.workbench.cdr.model;

import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;


@Entity
//TODO need to add a way to dynamically switch between database versions
//this dynamic connection will eliminate the need for the catalog attribute
@Table(name = "achilles_results_concept")
public class AnalysisResult  {

    private Long id;
    private Long analysisId;
    private String stratum1;
    private String stratum1Name;
    private String stratum2;
    private String stratum2Name;
    private String stratum3;
    private String stratum3Name;
    private String stratum4;
    private String stratum4Name;
    private String stratum5;
    private String stratum5Name;
    private Long countValue;

    @Id
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public AnalysisResult id(Long val) {
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
    public AnalysisResult analysisId(Long val) {
        this.analysisId = val;
        return this;
    }

    @Column(name="stratum_1")
    public String getStratum1() {
        return stratum1;
    }
    public void setStratum1(String stratum1) {
        this.stratum1 = stratum1;
    }
    public AnalysisResult stratum1(String val) {
        this.stratum1 = val;
        return this;
    }
    @Column(name="stratum_1_name")
    public String getStratum1Name() {
        return stratum1Name;
    }
    public void setStratum1Name(String stratum1Name) {
        this.stratum1Name = stratum1Name;
    }
    public AnalysisResult stratum1Name(String val) {
        this.stratum1Name = val;
        return this;
    }
    @Column(name="stratum_2")
    public String getStratum2() {
        return stratum2;
    }
    public void setStratum2(String stratum2) {
        this.stratum2 = stratum2;
    }
    public AnalysisResult stratum2(String val) {
        this.stratum2 = val;
        return this;
    }

    @Column(name="stratum_2_name")
    public String getStratum2Name() {
        return stratum2Name;
    }
    public void setStratum2Name(String stratum2Name) {
        this.stratum2Name = stratum2Name;
    }
    public AnalysisResult stratum2Name(String val) {
        this.stratum2Name = val;
        return this;
    }

    @Column(name="stratum_3")
    public String getStratum3() {
        return stratum3;
    }
    public void setStratum3(String stratum3) {
        this.stratum3 = stratum3;
    }
    public AnalysisResult stratum3(String val) {
        this.stratum3 = val;
        return this;
    }

    @Column(name="stratum_3_name")
    public String getStratum3Name() {
        return stratum3Name;
    }
    public void setStratum3Name(String stratum3Name) {
        this.stratum3Name = stratum3Name;
    }
    public AnalysisResult stratum3Name(String val) {
        this.stratum3Name = val;
        return this;
    }

    @Column(name="stratum_4")
    public String getStratum4() {
        return stratum4;
    }
    public void setStratum4(String stratum4) {
        this.stratum4 = stratum4;
    }
    public AnalysisResult stratum4(String val) {
        this.stratum4 = val;
        return this;
    }

    @Column(name="stratum_4_name")
    public String getStratum4Name() {
        return stratum4Name;
    }
    public void setStratum4Name(String stratum4Name) {
        this.stratum4Name = stratum4Name;
    }
    public AnalysisResult stratum4Name(String val) {
        this.stratum4Name = val;
        return this;
    }

    @Column(name="stratum_5")
    public String getStratum5() {
        return stratum5;
    }
    public void setStratum5(String stratum5) {
        this.stratum5 = stratum5;
    }
    public AnalysisResult stratum5(String val) {
        this.stratum5 = val;
        return this;
    }

    @Column(name="stratum_5_name")
    public String getStratum5Name() {
        return stratum5Name;
    }
    public void setStratum5Name(String stratum5Name) {
        this.stratum5Name = stratum5Name;
    }
    public AnalysisResult stratum5Name(String val) {
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
    public AnalysisResult countValue(Long val) {
        this.countValue = val;
        return this;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnalysisResult that = (AnalysisResult) o;
        return
                Objects.equals(analysisId, that.analysisId) &&
                Objects.equals(stratum1, that.stratum1) &&
                Objects.equals(stratum1Name, that.stratum1Name) &&
                Objects.equals(stratum2, that.stratum2) &&
                Objects.equals(stratum2Name, that.stratum2Name) &&
                Objects.equals(stratum3, that.stratum3) &&
                Objects.equals(stratum3Name, that.stratum3Name) &&
                Objects.equals(stratum4, that.stratum4) &&
                Objects.equals(stratum4Name, that.stratum4Name) &&
                Objects.equals(stratum5, that.stratum5) &&
                Objects.equals(stratum5Name, that.stratum5Name) &&
                Objects.equals(countValue, that.countValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, analysisId, stratum1, stratum1Name, stratum2, stratum2Name, stratum3, stratum3Name, stratum4, stratum4Name, stratum5, stratum5Name, countValue);
    }

    @Override
    public String toString() {
        return  ToStringBuilder.reflectionToString(this);
    }
}
