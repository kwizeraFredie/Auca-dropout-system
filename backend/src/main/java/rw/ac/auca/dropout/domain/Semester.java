package rw.ac.auca.dropout.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "semesters")
public class Semester {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @Column(name = "academic_year", nullable = false)
    private String academicYear;
    @Column(name = "starts_on", nullable = false)
    private LocalDate startsOn;
    @Column(name = "ends_on", nullable = false)
    private LocalDate endsOn;
    @Column(nullable = false)
    private boolean active;

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getAcademicYear() { return academicYear; }
    public boolean isActive() { return active; }
    public LocalDate getStartsOn() { return startsOn; }
    public LocalDate getEndsOn() { return endsOn; }
    public void setName(String value) { name = value; }
    public void setAcademicYear(String value) { academicYear = value; }
    public void setStartsOn(LocalDate value) { startsOn = value; }
    public void setEndsOn(LocalDate value) { endsOn = value; }
    public void setActive(boolean value) { active = value; }
}
