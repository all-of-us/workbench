package org.pmiops.workbench.cdr.model;

import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Objects;


@Entity
//TODO need to add a way to dynamically switch between database versions
//this dynamic connection will eliminate the need for the catalog attribute
@Table(name = "achilles_analysis")
public class AchillesAnalysis {


    private Long analysisId;
    private String analysisName;
    private String stratum1Name;
    private String stratum2Name;
    private String stratum3Name;
    private String stratum4Name;
    private String stratum5Name;
    private String chartType;
    private String dataType;


    @Id
    @Column(name="analysis_id")
    public Long getAnalysisId() {
        return analysisId;
    }
    public void setAnalysisId(Long analysisId) {
        this.analysisId = analysisId;
    }
    public AchillesAnalysis analysisId(Long val) {
        this.analysisId = val;
        return this;
    }

    @Column(name="analysis_name")
    public String getAnalysisName() {
        return analysisName;
    }
    public void setAnalysisName(String analysisName) {
        this.analysisName = analysisName;
    }
    public AchillesAnalysis analysisName(String val) {
        this.analysisName = val;
        return this;
    }

    @Column(name="stratum_1_name")
    public String getStratum1Name() {
        return stratum1Name;
    }
    public void setStratum1Name(String stratum1Name) {
        this.stratum1Name = stratum1Name;
    }
    public AchillesAnalysis stratum1Name(String val) {
        this.stratum1Name = val;
        return this;
    }

    @Column(name="stratum_2_name")
    public String getStratum2Name() {
        return stratum2Name;
    }
    public void setStratum2Name(String stratum2Name) {
        this.stratum2Name = stratum2Name;
    }
    public AchillesAnalysis stratum2Name(String val) {
        this.stratum2Name = val;
        return this;
    }

    @Column(name="stratum_3_name")
    public String getStratum3Name() {
        return stratum3Name;
    }
    public void setStratum3Name(String stratum3Name) {
        this.stratum3Name = stratum3Name;
    }
    public AchillesAnalysis stratum3Name(String val) {
        this.stratum3Name = val;
        return this;
    }

    @Column(name="stratum_4_name")
    public String getStratum4Name() {
        return stratum4Name;
    }
    public void setStratum4Name(String stratum4Name) {
        this.stratum4Name = stratum4Name;
    }
    public AchillesAnalysis stratum4Name(String val) {
        this.stratum4Name = val;
        return this;
    }

    @Column(name="stratum_5_name")
    public String getStratum5Name() {
        return stratum5Name;
    }
    public void setStratum5Name(String stratum5Name) {
        this.stratum5Name = stratum5Name;
    }
    public AchillesAnalysis stratum5Name(String val) {
        this.stratum5Name = val;
        return this;
    }

    @Column(name="chart_type")
    public String getChartType() {
        return chartType;
    }
    public void setChartType(String stratum5) {
        this.chartType = chartType;
    }
    public AchillesAnalysis chartType(String val) {
        this.chartType = val;
        return this;
    }

    @Column(name="data_type")
    public String getDataType() {
        return dataType;
    }
    public void setDataType(String dataType) {
        this.dataType = dataType;
    }
    public AchillesAnalysis dataType(String val) {
        this.dataType = val;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AchillesAnalysis that = (AchillesAnalysis) o;
        return Objects.equals(analysisId, that.analysisId) &&
                Objects.equals(analysisName, that.analysisName) &&
                Objects.equals(stratum1Name, that.stratum1Name) &&
                Objects.equals(stratum2Name, that.stratum2Name) &&
                Objects.equals(stratum3Name, that.stratum3Name) &&
                Objects.equals(stratum4Name, that.stratum4Name) &&
                Objects.equals(stratum5Name, that.stratum5Name) &&
                Objects.equals(chartType, that.chartType) &&
                Objects.equals(dataType, that.dataType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(analysisId, analysisName, stratum1Name, stratum2Name, stratum3Name, stratum4Name, stratum5Name, chartType, dataType);
    }

    @Override
    public String toString() {
        return  ToStringBuilder.reflectionToString(this);
    }
}
