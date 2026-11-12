package com.example.HealthCare.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class DashboardStatsDTO {

    private String role;

    private long totalPatients;
    private long totalMedecins;
    private long totalDossiers;

    private long totalRendezVous;
    private long todayRendezVous;
    private long upcomingRendezVous;

    private Map<String, Long> rendezVousByStatus = new LinkedHashMap<>();
    private Map<String, Long> medecinsBySpecialite = new LinkedHashMap<>();
    private List<AppointmentRow> recentAppointments = new ArrayList<>();

    // PATIENT only
    private PatientResponseDTO profile;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AppointmentRow {
        private Long id;
        private LocalDateTime dateRendezVous;
        private String statut;
        private String patientNom;
        private String medecinNom;
        private String specialite;
    }
}