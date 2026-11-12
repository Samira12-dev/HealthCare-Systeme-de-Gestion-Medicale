package com.example.HealthCare.seed;

import com.example.HealthCare.entity.*;
import com.example.HealthCare.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 *   admin   : admin@healthcare.com   / Admin123!
 *   doctors : <x>@healthcare.com     / Doctor123!
 *   patients: <x>@mail.com           / Patient123!
 * Disable with  app.seed.enabled=false
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepo userRepo;
    private final PatientRepo patientRepo;
    private final MedecinRepo medecinRepo;
    private final RendezVousRepo rendezVousRepo;
    private final DossierMedicalRepo dossierMedicalRepo;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.enabled:true}")
    private boolean enabled;

    public DataSeeder(UserRepo userRepo, PatientRepo patientRepo, MedecinRepo medecinRepo,
                      RendezVousRepo rendezVousRepo, DossierMedicalRepo dossierMedicalRepo,
                      PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.patientRepo = patientRepo;
        this.medecinRepo = medecinRepo;
        this.rendezVousRepo = rendezVousRepo;
        this.dossierMedicalRepo = dossierMedicalRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!enabled) return;

        if (userRepo.findByEmail("admin@healthcare.com").isEmpty()) {
            Admin admin = new Admin();
            admin.setUsername("admin");
            admin.setEmail("admin@healthcare.com");
            admin.setPassword(passwordEncoder.encode("Admin123!"));
            admin.setRole(Role.ADMIN);
            userRepo.save(admin);
            log.info("Seed: admin account created (admin@healthcare.com / Admin123!)");
        }

        if (medecinRepo.count() > 0 || patientRepo.count() > 0) return;

        Medecin m1 = medecin("Benali", "Cardiologie", "benali@healthcare.com", "0600000001");
        Medecin m2 = medecin("Alaoui", "Pédiatrie", "alaoui@healthcare.com", "0600000002");
        Medecin m3 = medecin("Idrissi", "Dermatologie", "idrissi@healthcare.com", "0600000003");

        Patient p1 = patient("Amrani", "Sara", "sara@mail.com", "0611111111", LocalDate.of(1992, 4, 12));
        Patient p2 = patient("Tazi", "Youssef", "youssef@mail.com", "0622222222", LocalDate.of(1985, 9, 3));
        Patient p3 = patient("Fassi", "Lina", "lina@mail.com", "0633333333", LocalDate.of(2001, 1, 25));
        Patient p4 = patient("Chraibi", "Omar", "omar@mail.com", "0644444444", LocalDate.of(1978, 11, 30));

        LocalDateTime base = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
        rdv(p1, m1, base.plusDays(1).withHour(9), StatutRendezVous.PLANIFIE);
        rdv(p2, m2, base.plusDays(2).withHour(11), StatutRendezVous.CONFIRME);
        rdv(p3, m3, base.withHour(15), StatutRendezVous.CONFIRME);
        rdv(p4, m1, base.minusDays(3).withHour(10), StatutRendezVous.TERMINE);
        rdv(p1, m3, base.minusDays(7).withHour(14), StatutRendezVous.ANNULE);
        rdv(p2, m1, base.plusDays(5).withHour(16), StatutRendezVous.PLANIFIE);

        dossier(p1, "Hypertension légère", "Contrôle dans 3 mois");
        dossier(p4, "Suivi cardiaque", "Régime et activité physique recommandés");

        log.info("Seed: demo doctors, patients, appointments and records created");
    }

    private Medecin medecin(String nom, String specialite, String email, String tel) {
        Medecin m = new Medecin();
        m.setNom(nom);
        m.setSpecialite(specialite);
        m.setTelephone(tel);
        m.setUsername(nom);
        m.setEmail(email);
        m.setPassword(passwordEncoder.encode("Doctor123!"));
        m.setRole(Role.MEDECIN);
        return medecinRepo.save(m);
    }

    private Patient patient(String nom, String prenom, String email, String tel, LocalDate dob) {
        Patient p = new Patient();
        p.setNom(nom);
        p.setPrenom(prenom);
        p.setTelephone(tel);
        p.setDateNaissance(dob);
        p.setUsername(nom);
        p.setEmail(email);
        p.setPassword(passwordEncoder.encode("Patient123!"));
        p.setRole(Role.PATIENT);
        return patientRepo.save(p);
    }

    private void rdv(Patient p, Medecin m, LocalDateTime date, StatutRendezVous statut) {
        RendezVous r = new RendezVous();
        r.setPatient(p);
        r.setMedecin(m);
        r.setDateRendezVous(date);
        r.setStatut(statut);
        rendezVousRepo.save(r);
    }

    private void dossier(Patient p, String diagnostic, String observation) {
        DossierMedical d = new DossierMedical();
        d.setPatient(p);
        d.setDiagnostic(diagnostic);
        d.setObservation(observation);
        d.setDateCreation(LocalDate.now());
        dossierMedicalRepo.save(d);
    }
}