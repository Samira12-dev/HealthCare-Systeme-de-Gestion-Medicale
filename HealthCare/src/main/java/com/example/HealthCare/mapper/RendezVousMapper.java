package com.example.HealthCare.mapper;

import com.example.HealthCare.dto.RendezVouRequestDTO;
import com.example.HealthCare.dto.RendezVousResponseDTO;
import com.example.HealthCare.entity.RendezVous;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RendezVousMapper {

    @Mapping(source = "patient.id", target = "patientId")
    @Mapping(source = "medecin.id", target = "medecinId")
    @Mapping(source = "patient.nom", target = "patientNom")
    @Mapping(source = "patient.prenom", target = "patientPrenom")
    @Mapping(source = "medecin.nom", target = "medecinNom")
    @Mapping(source = "medecin.specialite", target = "medecinSpecialite")
    RendezVousResponseDTO toDto(RendezVous rendezVous);
    List<RendezVousResponseDTO>toDto(List<RendezVous>rendezVousList);
    RendezVous toEntity(RendezVouRequestDTO rendezVousRequestDTO);

    void update(RendezVouRequestDTO requestDTO, @MappingTarget RendezVous rendezVous);
}
