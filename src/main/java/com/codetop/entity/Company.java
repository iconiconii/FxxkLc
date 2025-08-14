package com.codetop.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Set;

/**
 * Company entity representing companies that ask algorithm problems in interviews.
 * 
 * Features:
 * - Company profile information (name, website, location)
 * - Association with problems through ProblemCompany entity
 * - Support for company logos and branding
 * 
 * @author CodeTop Team
 */
@TableName("companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company extends BaseEntity {

    @NotBlank
    @Size(min = 1, max = 100)
    @TableField("name")
    private String name;

    @TableField("display_name")
    private String displayName;

    @TableField("website_url")
    private String website;

    @TableField("location")
    private String location;

    @TableField("logo_url")
    private String logoUrl;

    @TableField("description")
    private String description;

    @Builder.Default
    @TableField("is_active")
    private Boolean isActive = true;

    @TableField("industry")
    private String industry;

    @TableField("size_category")
    private String sizeCategory; // STARTUP, SMALL, MEDIUM, LARGE, ENTERPRISE

    @TableField("stock_symbol")
    private String stockSymbol;

    // Associations (not mapped to database in MyBatis-Plus)
    @TableField(exist = false)
    @JsonIgnore
    private Set<ProblemCompany> problemCompanies;

    // Derived fields
    public String getEffectiveDisplayName() {
        return displayName != null ? displayName : name;
    }

    public boolean hasWebsite() {
        return website != null && !website.trim().isEmpty();
    }

    public boolean hasLogo() {
        return logoUrl != null && !logoUrl.trim().isEmpty();
    }

    /**
     * Deactivate company (soft delete).
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * Activate company.
     */
    public void activate() {
        this.isActive = true;
    }

    @Override
    public String toString() {
        return "Company{" +
                "id=" + getId() +
                ", name='" + name + '\'' +
                ", displayName='" + displayName + '\'' +
                ", location='" + location + '\'' +
                ", industry='" + industry + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}