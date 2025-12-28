package com.wohngeld.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PdfResult {

    private String outputPath;
    private String filename;
    private int fieldsFound;
    private int fieldsFilled;
}
