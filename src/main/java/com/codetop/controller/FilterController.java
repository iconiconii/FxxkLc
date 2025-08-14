package com.codetop.controller;

import com.codetop.entity.Company;
import com.codetop.entity.Department;
import com.codetop.entity.Position;
import com.codetop.mapper.CompanyMapper;
import com.codetop.mapper.DepartmentMapper;
import com.codetop.mapper.PositionMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Progressive filtering API controller for company → department → position cascade.
 * 
 * Provides endpoints for:
 * - Getting all companies for initial dropdown
 * - Getting departments for selected company
 * - Getting positions for selected department
 * 
 * @author CodeTop Team
 */
@RestController
@RequestMapping("/filter")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Progressive Filtering", description = "APIs for cascade filtering: company → department → position")
public class FilterController {

    private final CompanyMapper companyMapper;
    private final DepartmentMapper departmentMapper;
    private final PositionMapper positionMapper;

    @Operation(
        summary = "Get all companies",
        description = "Get list of all active companies for dropdown selection"
    )
    @GetMapping("/companies")
    public ResponseEntity<List<Company>> getAllCompanies() {
        log.info("Fetching all active companies for dropdown");
        
        List<Company> companies = companyMapper.findAllActiveCompanies();
        
        log.info("Found {} active companies", companies.size());
        return ResponseEntity.ok(companies);
    }

    @Operation(
        summary = "Get departments by company",
        description = "Get list of departments for the specified company"
    )
    @GetMapping("/companies/{companyId}/departments")
    public ResponseEntity<List<Department>> getDepartmentsByCompany(
            @Parameter(description = "Company ID") 
            @PathVariable Long companyId) {
        
        log.info("Fetching departments for company ID: {}", companyId);
        
        List<Department> departments = departmentMapper.findByCompanyId(companyId);
        
        log.info("Found {} departments for company ID: {}", departments.size(), companyId);
        return ResponseEntity.ok(departments);
    }

    @Operation(
        summary = "Get positions by department",
        description = "Get list of positions for the specified department"
    )
    @GetMapping("/departments/{departmentId}/positions")
    public ResponseEntity<List<Position>> getPositionsByDepartment(
            @Parameter(description = "Department ID") 
            @PathVariable Long departmentId) {
        
        log.info("Fetching positions for department ID: {}", departmentId);
        
        List<Position> positions = positionMapper.findByDepartmentId(departmentId);
        
        log.info("Found {} positions for department ID: {}", positions.size(), departmentId);
        return ResponseEntity.ok(positions);
    }

    @Operation(
        summary = "Get positions by company and department",
        description = "Get list of positions for the specified company and department combination"
    )
    @GetMapping("/companies/{companyId}/departments/{departmentId}/positions")
    public ResponseEntity<List<Position>> getPositionsByCompanyAndDepartment(
            @Parameter(description = "Company ID") 
            @PathVariable Long companyId,
            @Parameter(description = "Department ID") 
            @PathVariable Long departmentId) {
        
        log.info("Fetching positions for company ID: {} and department ID: {}", companyId, departmentId);
        
        List<Position> positions = positionMapper.findByCompanyAndDepartment(companyId, departmentId);
        
        log.info("Found {} positions for company ID: {} and department ID: {}", 
                positions.size(), companyId, departmentId);
        return ResponseEntity.ok(positions);
    }

    @Operation(
        summary = "Health check for filtering system",
        description = "Check if the filtering system is working properly"
    )
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        try {
            // Test basic functionality
            List<Company> companies = companyMapper.findAllActiveCompanies();
            log.info("Filter system health check passed - found {} companies", companies.size());
            return ResponseEntity.ok("Filter system is healthy");
        } catch (Exception e) {
            log.error("Filter system health check failed", e);
            return ResponseEntity.status(500).body("Filter system is experiencing issues");
        }
    }
}