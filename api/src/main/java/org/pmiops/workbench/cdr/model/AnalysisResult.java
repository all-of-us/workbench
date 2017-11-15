package org.pmiops.workbench.cdr.model;

import javax.persistence.*;
import java.io.Serializable;


@Entity
//TODO need to add a way to dynamically switch between database versions
//this dynamic connection will eliminate the need for the catalog attribute
@Table(name = "ACHILLES_results_view", catalog="cdr")
public class AnalysisResult  {


    @Id private Long id;
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
    public String toString() {
        return "AnalysisResult{" +
                "analysisId=" + analysisId +
                ", stratum1='" + stratum1 + '\'' +
                ", stratum1Name='" + stratum1Name + '\'' +
                ", stratum2='" + stratum2 + '\'' +
                ", stratum2Name='" + stratum2Name + '\'' +
                ", stratum3='" + stratum3 + '\'' +
                ", stratum3Name='" + stratum3Name + '\'' +
                ", stratum4='" + stratum4 + '\'' +
                ", stratum4Name='" + stratum4Name + '\'' +
                ", stratum5='" + stratum5 + '\'' +
                ", stratum5Name='" + stratum5Name + '\'' +
                ", countValue=" + countValue +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AnalysisResult that = (AnalysisResult) o;

        if (analysisId != that.analysisId) return false;
        if (countValue != that.countValue) return false;
        if (stratum1 != null ? !stratum1.equals(that.stratum1) : that.stratum1 != null) return false;
        if (stratum1Name != null ? !stratum1Name.equals(that.stratum1Name) : that.stratum1Name != null) return false;
        if (stratum2 != null ? !stratum2.equals(that.stratum2) : that.stratum2 != null) return false;
        if (stratum2Name != null ? !stratum2Name.equals(that.stratum2Name) : that.stratum2Name != null) return false;
        if (stratum3 != null ? !stratum3.equals(that.stratum3) : that.stratum3 != null) return false;
        if (stratum3Name != null ? !stratum3Name.equals(that.stratum3Name) : that.stratum3Name != null) return false;
        if (stratum4 != null ? !stratum4.equals(that.stratum4) : that.stratum4 != null) return false;
        if (stratum4Name != null ? !stratum4Name.equals(that.stratum4Name) : that.stratum4Name != null) return false;
        if (stratum5 != null ? !stratum5.equals(that.stratum5) : that.stratum5 != null) return false;
        return stratum5Name != null ? stratum5Name.equals(that.stratum5Name) : that.stratum5Name == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (analysisId ^ (analysisId >>> 32));
        result = 31 * result + (stratum1 != null ? stratum1.hashCode() : 0);
        result = 31 * result + (stratum1Name != null ? stratum1Name.hashCode() : 0);
        result = 31 * result + (stratum2 != null ? stratum2.hashCode() : 0);
        result = 31 * result + (stratum2Name != null ? stratum2Name.hashCode() : 0);
        result = 31 * result + (stratum3 != null ? stratum3.hashCode() : 0);
        result = 31 * result + (stratum3Name != null ? stratum3Name.hashCode() : 0);
        result = 31 * result + (stratum4 != null ? stratum4.hashCode() : 0);
        result = 31 * result + (stratum4Name != null ? stratum4Name.hashCode() : 0);
        result = 31 * result + (stratum5 != null ? stratum5.hashCode() : 0);
        result = 31 * result + (stratum5Name != null ? stratum5Name.hashCode() : 0);
        result = 31 * result + (int) (countValue ^ (countValue >>> 32));
        return result;
    }
}
