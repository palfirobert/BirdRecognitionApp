package com.example.birdrecognitionapp.dto;

import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


public class SoundPredictionResponse implements Comparable<SoundPredictionResponse>{
    private String common_name;
    private String scientific_name;
    private Long start_time;
    private Long end_time;
    private Double confidence;
    private String label;

    public SoundPredictionResponse(String common_name, String scientific_name, Long start_time, Long end_time, Double confidence, String label) {
        this.common_name = common_name;
        this.scientific_name = scientific_name;
        this.start_time = start_time;
        this.end_time = end_time;
        this.confidence = confidence;
        this.label = label;
    }

    public SoundPredictionResponse(){}
    public String getCommon_name() {
        return common_name;
    }

    public void setCommon_name(String common_name) {
        this.common_name = common_name;
    }

    public String getScientific_name() {
        return scientific_name;
    }

    public void setScientific_name(String scientific_name) {
        this.scientific_name = scientific_name;
    }

    public Long getStart_time() {
        return start_time;
    }

    public void setStart_time(Long start_time) {
        this.start_time = start_time;
    }

    public Long getEnd_time() {
        return end_time;
    }

    public void setEnd_time(Long end_time) {
        this.end_time = end_time;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return "SoundPredictionResponse{" +
                "common_name='" + common_name + '\'' +
                ", scientific_name='" + scientific_name + '\'' +
                ", start_time=" + start_time +
                ", end_time=" + end_time +
                ", confidence=" + confidence +
                ", label='" + label + '\'' +
                '}';
    }

    @Override
    public int compareTo(SoundPredictionResponse o) {
        return Double.compare(this.confidence,o.getConfidence());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SoundPredictionResponse that = (SoundPredictionResponse) o;
        return Objects.equals(common_name, that.common_name) && Objects.equals(scientific_name, that.scientific_name) && Objects.equals(start_time, that.start_time) && Objects.equals(end_time, that.end_time) && Objects.equals(confidence, that.confidence) && Objects.equals(label, that.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(common_name, scientific_name, start_time, end_time, confidence, label);
    }
}
