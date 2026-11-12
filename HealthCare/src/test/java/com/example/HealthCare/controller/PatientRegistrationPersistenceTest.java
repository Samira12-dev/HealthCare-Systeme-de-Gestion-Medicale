package com.example.HealthCare.controller;

import com.example.HealthCare.config.JwtUtils;
import com.example.HealthCare.dto.PatientResponseDTO;
import com.example.HealthCare.dto.RegisterRequestDto;
import com.example.HealthCare.dto.MedecinResponseDTO;
import com.example.HealthCare.entity.Medecin;
import com.example.HealthCare.entity.Patient;
import com.example.HealthCare.entity.Role;
import com.example.HealthCare.entity.User;
import com.example.HealthCare.mapper.MedecinMapper;
import com.example.HealthCare.mapper.PatientMapper;
import com.example.HealthCare.repository.MedecinRepo;
import com.example.HealthCare.repository.PatientRepo;
import com.example.HealthCare.repository.UserRepo;
import com.example.HealthCare.service.MedecinService;
import com.example.HealthCare.service.PatientService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:healthcare-test;MODE=MySQL;NON_KEYWORDS=USER",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PatientRegistrationPersistenceTest {

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private PatientRepo patientRepo;

    @Autowired
    private MedecinRepo medecinRepo;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void registrationPersistsJoinedPatientAndMyProfileFindsIt() {
        String email = "patient@example.com";
        JwtUtils jwtUtils = mock(JwtUtils.class);
        when(jwtUtils.generateToken(email, Role.PATIENT.name())).thenReturn("token");
        AuthController authController = new AuthController(
                new BCryptPasswordEncoder(),
                userRepo,
                mock(AuthenticationManager.class),
                jwtUtils);

        RegisterRequestDto request = new RegisterRequestDto();
        request.setUsername("patient");
        request.setEmail(email);
        request.setPassword("secret123");
        request.setRole(Role.PATIENT);
        request.setNom("Patient");
        request.setPrenom("Example");

        assertEquals(HttpStatus.OK, authController.register(request).getStatusCode());

        User user = userRepo.findByEmail(email).orElseThrow();
        Patient patient = patientRepo.findByEmail(email).orElseThrow();
        assertInstanceOf(Patient.class, user);
        assertEquals(user.getId(), patient.getId());

        PatientResponseDTO expectedProfile = new PatientResponseDTO();
        expectedProfile.setId(patient.getId());
        expectedProfile.setNom(patient.getNom());
        expectedProfile.setPrenom(patient.getPrenom());
        PatientMapper patientMapper = mock(PatientMapper.class);
        when(patientMapper.toDto(patient)).thenReturn(expectedProfile);
        PatientController patientController = new PatientController(
                new PatientService(patientRepo, patientMapper));
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(email);

        assertSame(expectedProfile, patientController.getMyProfile(authentication).getBody());
    }

    @Test
    void missingPatientProfileReturnsNotFound() {
        PatientService patientService = new PatientService(patientRepo, mock(PatientMapper.class));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> patientService.getPatientByEmail("missing@example.com"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void persistedMedecinSubtypeCanRetrieveOwnProfile() {
        Medecin medecin = new Medecin();
        medecin.setUsername("doctor");
        medecin.setEmail("doctor@example.com");
        medecin.setPassword("encoded-password");
        medecin.setRole(Role.MEDECIN);
        medecin.setNom("Doctor");
        medecin.setSpecialite("General");
        medecin = medecinRepo.saveAndFlush(medecin);

        User user = userRepo.findByEmail(medecin.getEmail()).orElseThrow();
        Medecin persistedMedecin = medecinRepo.findByEmail(medecin.getEmail()).orElseThrow();
        assertInstanceOf(Medecin.class, user);
        assertEquals(user.getId(), persistedMedecin.getId());

        MedecinResponseDTO expectedProfile = new MedecinResponseDTO();
        expectedProfile.setId(persistedMedecin.getId());
        expectedProfile.setNom(persistedMedecin.getNom());
        MedecinMapper medecinMapper = mock(MedecinMapper.class);
        when(medecinMapper.toDTO(persistedMedecin)).thenReturn(expectedProfile);
        MedecinController medecinController = new MedecinController(
                new MedecinService(medecinRepo, medecinMapper));
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(medecin.getEmail());

        assertSame(expectedProfile, medecinController.getMyProfile(authentication).getBody());
    }

    @Test
    void legacyPatientUserWithoutSubtypeRowCanBeBackfilledAndLoaded() {
        User legacyUser = new User();
        legacyUser.setUsername("legacy-patient");
        legacyUser.setEmail("legacy-patient@example.com");
        legacyUser.setPassword("encoded-password");
        legacyUser.setRole(Role.PATIENT);
        legacyUser = userRepo.saveAndFlush(legacyUser);

        int insertedProfiles = jdbcTemplate.update("""
                INSERT INTO patient (id, nom, prenom, telephone, date_naissance)
                SELECT u.id, NULL, NULL, NULL, NULL
                FROM `user` u
                LEFT JOIN patient p ON p.id = u.id
                WHERE u.id = ?
                  AND u.role = 'PATIENT'
                  AND p.id IS NULL
                """, legacyUser.getId());

        assertEquals(1, insertedProfiles);
        entityManager.clear();

        Patient patient = patientRepo.findByEmail(legacyUser.getEmail()).orElseThrow();
        assertEquals(legacyUser.getId(), patient.getId());

        PatientMapper patientMapper = mock(PatientMapper.class);
        PatientResponseDTO expectedProfile = new PatientResponseDTO();
        when(patientMapper.toDto(patient)).thenReturn(expectedProfile);
        PatientController patientController = new PatientController(
                new PatientService(patientRepo, patientMapper));
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(legacyUser.getEmail());

        assertSame(expectedProfile, patientController.getMyProfile(authentication).getBody());
    }
}
