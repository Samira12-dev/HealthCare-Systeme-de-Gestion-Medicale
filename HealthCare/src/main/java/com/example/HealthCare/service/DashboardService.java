package com.example.HealthCare.service;

import com.example.HealthCare.dto.DashboardStatsDTO;
import com.example.HealthCare.dto.DashboardStatsDTO.AppointmentRow;
import com.example.HealthCare.entity.*;
import com.example.HealthCare.mapper.PatientMapper;
import com.example.HealthCare.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final UserRepo userRepo;
    private final PatientRepo patientRepo;
    private final MedecinRepo medecinRepo;
    private final RendezVousRepo rendezVousRepo;
    private final DossierMedicalRepo dossierMedicalRepo;
    private final PatientMapper patientMapper;

    public DashboardService(UserRepo userRepo,
                            PatientRepo patientRepo,
                            MedecinRepo medecinRepo,
                            RendezVousRepo rendezVousRepo,
                            DossierMedicalRepo dossierMedicalRepo,
                            PatientMapper patientMapper) {
        this.userRepo = userRepo;
        this.patientRepo = patientRepo;
        this.medecinRepo = medecinRepo;
        this.rendezVousRepo = rendezVousRepo;
        this.dossierMedicalRepo = dossierMedicalRepo;
        this.patientMapper = patientMapper;
    }

    @Transactional
    public DashboardStatsDTO getStats(String email) {

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        DashboardStatsDTO dto = new DashboardStatsDTO();
        dto.setRole(user.getRole().name());

        List<RendezVous> scope;

        if (user.getRole() == Role.PATIENT) {
            patientRepo.findByEmail(email).ifPresent(p -> dto.setProfile(patientMapper.toDto(p)));
            scope = patientRepo.findByEmail(email)
                    .map(p -> rendezVousRepo.findByPatientId(p.getId()))
                    .orElseGet(ArrayList::new);
        } else {
            dto.setTotalPatients(patientRepo.count());
            dto.setTotalMedecins(medecinRepo.count());
            dto.setTotalDossiers(dossierMedicalRepo.count());

            Map<String, Long> bySpec = new TreeMap<>();
            for (Medecin m : medecinRepo.findAll()) {
                String spec = m.getSpecialite() == null ? "—" : m.getSpecialite();
                bySpec.merge(spec, 1L, Long::sum);
            }
            dto.setMedecinsBySpecialite(new LinkedHashMap<>(bySpec));

            if (user.getRole() == Role.MEDECIN) {
                scope = medecinRepo.findByEmail(email)
                        .map(m -> rendezVousRepo.findByMedecinId(m.getId()))
                        .orElseGet(ArrayList::new);
            } else {
                scope = rendezVousRepo.findAll();
            }
        }

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (StatutRendezVous s : StatutRendezVous.values()) {
            byStatus.put(s.name(), 0L);
        }

        long todayCount = 0;
        long upcoming = 0;
        for (RendezVous r : scope) {
            if (r.getStatut() != null) {
                byStatus.merge(r.getStatut().name(), 1L, Long::sum);
            }
            if (r.getDateRendezVous() != null) {
                if (r.getDateRendezVous().toLocalDate().equals(today)) todayCount++;
                if (r.getDateRendezVous().isAfter(now)
                        && r.getStatut() != StatutRendezVous.ANNULE
                        && r.getStatut() != StatutRendezVous.TERMINE) {
                    upcoming++;
                }
            }
        }

        dto.setTotalRendezVous(scope.size());
        dto.setTodayRendezVous(todayCount);
        dto.setUpcomingRendezVous(upcoming);
        dto.setRendezVousByStatus(byStatus);

        List<AppointmentRow> rows = scope.stream()
                .sorted(Comparator.comparing(RendezVous::getDateRendezVous,
                        Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder())))
                .limit(6)
                .map(r -> new AppointmentRow(
                        r.getId(),
                        r.getDateRendezVous(),
                        r.getStatut() == null ? null : r.getStatut().name(),
                        fullName(r.getPatient()),
                        r.getMedecin() == null ? null : r.getMedecin().getNom(),
                        r.getMedecin() == null ? null : r.getMedecin().getSpecialite()
                ))
                .collect(Collectors.toList());
        dto.setRecentAppointments(rows);

        return dto;
    }

    private String fullName(Patient p) {
        if (p == null) return null;
        String prenom = p.getPrenom() == null ? "" : p.getPrenom();
        String nom = p.getNom() == null ? "" : p.getNom();
        String full = (prenom + " " + nom).trim();
        return full.isEmpty() ? p.getEmail() : full;
    }
}