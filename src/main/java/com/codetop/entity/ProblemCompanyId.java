package com.codetop.entity;

import lombok.*;

import java.io.Serializable;

/**
 * Composite primary key for ProblemCompany entity.
 * 
 * @author CodeTop Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ProblemCompanyId implements Serializable {
    private Long problemId;
    private Long companyId;
}