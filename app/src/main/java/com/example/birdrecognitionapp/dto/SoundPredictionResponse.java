package com.example.birdrecognitionapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


public class SoundPredictionResponse {
    private String common_name;
    private String scientific_name;
    private Long start_time;
    private Long end_time;
    private Double confidence;
    private String label;

    public SoundPredictionResponse(String commonName, String scientificName, int startTime, int endTime, double confidence, String label) {
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
}
