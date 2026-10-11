package com.campus.library.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Objects;

/**
 * Campus library facility / branch master directory.
 * E.g., 'CENTRAL', 'CS-DEPT', 'MECH-DEPT'
 */
@Entity
@Table(
    name = "branches",
    indexes = {
        @Index(name = "idx_branch_code", columnList = "code", unique = true)
    }
)
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Branch code cannot be blank")
    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @NotBlank(message = "Branch name cannot be blank")
    @Column(nullable = false, length = 100)
    private String name;

    @Email(message = "Must be a valid contact email")
    @NotBlank(message = "Contact email cannot be blank")
    @Column(name = "contact_email", nullable = false)
    private String contactEmail;

    public Branch() {
    }

    public Branch(String code, String name, String contactEmail) {
        this.code = code;
        this.name = name;
        this.contactEmail = contactEmail;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Branch branch = (Branch) o;
        return Objects.equals(code, branch.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }
}
