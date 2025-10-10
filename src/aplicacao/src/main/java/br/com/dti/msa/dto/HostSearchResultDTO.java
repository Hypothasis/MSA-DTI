package br.com.dti.msa.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HostSearchResultDTO {
    private String publicId;
    private String name;
}