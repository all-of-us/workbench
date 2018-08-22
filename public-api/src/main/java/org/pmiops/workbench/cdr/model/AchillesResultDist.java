package org.pmiops.workbench.cdr.model;

import javax.persistence.*;
import java.util.Objects;

@Entity
//TODO need to add a way to dynamically switch between database versions
//this dynamic connection will eliminate the need for the catalog attribute
@Table(name = "achilles_results_dist")
public class AchillesResultDist  {

    private Long id;
    private Long analysisId;
    private String stratum1;
    private String stratum2;
    private String stratum3;
    private String stratum4;
    private String stratum5;
    private Long countValue;
    private Float minValue;
    private Float maxValue;
    private Float avgValue;
    private Float stdevValue;
    private Float medianValue;
    private Float p10Value;
    private Float p25Value;
    private Float p75Value;
    private Float p90Value;

    @Id
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public AchillesResultDist id(Long val) {
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
    public AchillesResultDist analysisId(Long val) {
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
    public AchillesResultDist stratum1(String val) {
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
    public AchillesResultDist stratum2(String val) {
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
    public AchillesResultDist stratum3(String val) {
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
    public AchillesResultDist stratum4(String val) {
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
    public AchillesResultDist stratum5(String val) {
        this.stratum5 = val;
        return this;
    }

    @Column(name="count_value")
    public Long getCountValue() {
        return countValue;
    }
    public void setCountValue(Long countValue) {
        this.countValue = countValue;
    }
    public AchillesResultDist countValue(Long val) {
        this.countValue = val;
        return this;
    }

    @Column(name="min_value")
    public Float getMinValue() {
        return minValue;
    }
    public void setMinValue(float minValue) {
        this.minValue = minValue;
    }
    public AchillesResultDist minValue(Float val) {
        this.minValue = val;
        return this;
    }

    @Column(name="max_value")
    public Float getMaxValue() {
        return maxValue;
    }
    public void setMaxValue(Float maxValue) {
        this.maxValue = maxValue;
    }
    public AchillesResultDist maxValue(Float val) {
        this.maxValue = val;
        return this;
    }

    @Column(name="avg_value")
    public float getAvgValue() {
        return avgValue;
    }
    public void setAvgValue(float avgValue) {
        this.avgValue = avgValue;
    }
    public AchillesResultDist avgValue(float val) {
        this.avgValue = val;
        return this;
    }

    @Column(name="stdev_value")
    public float getStdevValue() {
        return stdevValue;
    }
    public void setStdevValue(float stdevValue) {
        this.stdevValue = stdevValue;
    }
    public AchillesResultDist stdevValue(float val) {
        this.stdevValue = val;
        return this;
    }

    @Column(name="median_value")
    public float getMedianValue() {
        return medianValue;
    }
    public void setMedianValue(float medianValue) {
        this.medianValue = medianValue;
    }
    public AchillesResultDist medianValue(float val) {
        this.medianValue = val;
        return this;
    }

    @Column(name="p10_value")
    public float getP10Value() {
        return p10Value;
    }
    public void setP10Value(float p10Value) {
        this.p10Value = p10Value;
    }
    public AchillesResultDist p10Value(float val) {
        this.p10Value = val;
        return this;
    }

    @Column(name="p25_value")
    public float getP25Value() {
        return p25Value;
    }
    public void setP25Value(float p25Value) {
        this.p25Value = p25Value;
    }
    public AchillesResultDist p25Value(float val) {
        this.p25Value = val;
        return this;
    }

    @Column(name="p75_value")
    public float getP75Value() {
        return p75Value;
    }
    public void setP75Value(float p75Value) {
        this.p75Value = p75Value;
    }
    public AchillesResultDist p75Value(float val) {
        this.p75Value = val;
        return this;
    }

    @Column(name="p90_value")
    public float getP90Value() {
        return p90Value;
    }
    public void setP90Value(float p90Value) {
        this.p90Value = p90Value;
    }
    public AchillesResultDist p90Value(float val) {
        this.p90Value = val;
        return this;
    }

    @Override
    public String toString() {
        return "AchillesResultDist{" +
                "id=" + id +
                ", analysisId=" + analysisId +
                ", stratum1='" + stratum1 + '\'' +
                ", stratum2='" + stratum2 + '\'' +
                ", stratum3='" + stratum3 + '\'' +
                ", stratum4='" + stratum4 + '\'' +
                ", stratum5='" + stratum5 + '\'' +
                ", countValue=" + countValue +
                ", minValue=" + minValue +
                ", maxValue=" + maxValue +
                ", avgValue=" + avgValue +
                ", stdevValue=" + stdevValue +
                ", medianValue=" + medianValue +
                ", p10Value=" + p10Value +
                ", p25Value=" + p25Value +
                ", p75Value=" + p75Value +
                ", p90Value=" + p90Value +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AchillesResultDist that = (AchillesResultDist) o;
        return analysisId == that.analysisId &&
                Objects.equals(stratum1, that.stratum1) &&
                Objects.equals(stratum2, that.stratum2) &&
                Objects.equals(stratum3, that.stratum3) &&
                Objects.equals(stratum4, that.stratum4) &&
                Objects.equals(stratum5, that.stratum5) &&
                Objects.equals(countValue, that.countValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(analysisId, stratum1, stratum2, stratum3, stratum4, stratum5, countValue, minValue, maxValue, avgValue);
    }
}
