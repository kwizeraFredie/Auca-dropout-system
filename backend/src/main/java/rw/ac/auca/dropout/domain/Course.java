package rw.ac.auca.dropout.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "courses")
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String code;
    @Column(nullable = false)
    private String title;
    @Column(nullable = false)
    private int credits;
    @Column(name = "department_id", nullable = false)
    private Long departmentId;
    @Column(name = "professor_id")
    private java.util.UUID professorId;

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getTitle() { return title; }
    public int getCredits() { return credits; }
    public Long getDepartmentId() { return departmentId; }
    public java.util.UUID getProfessorId() { return professorId; }
    public void setCode(String value) { code = value; }
    public void setTitle(String value) { title = value; }
    public void setCredits(int value) { credits = value; }
    public void setDepartmentId(Long value) { departmentId = value; }
    public void setProfessorId(java.util.UUID value) { professorId = value; }
}
