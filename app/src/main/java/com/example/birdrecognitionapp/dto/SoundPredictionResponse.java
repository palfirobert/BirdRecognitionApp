package com.example.birdrecognitionapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SoundPredictionResponse {
    private String common_name;
    private String scientific_name;
    private Long start_time;
    private Long end_time;
    private Double confidence;
    private String label;

}
