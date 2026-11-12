package com.example.HealthCare.mapper;

import com.example.HealthCare.dto.DossierMedicalRequestDto;
import com.example.HealthCare.dto.DossierMedicalResponseDto;
import com.example.HealthCare.entity.DossierMedical;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DossierMedicalMapper {

    @Mapping(source = "patient.id", target = "patientId")
    @Mapping(source = "patient.nom", target = "patientNom")
    @Mapping(source = "patient.prenom", target = "patientPrenom")
    DossierMedicalResponseDto toDto(DossierMedical dossierMedical);

    DossierMedical toEntity(DossierMedicalRequestDto dossierMedicalRequestDto);
}