ALTER TABLE semesters
    ADD CONSTRAINT semesters_valid_date_range
    CHECK (ends_on > starts_on);

CREATE UNIQUE INDEX semesters_one_active
    ON semesters (active)
    WHERE active = TRUE;

CREATE OR REPLACE FUNCTION enforce_max_six_courses()
RETURNS TRIGGER AS $$
BEGIN
    PERFORM pg_advisory_xact_lock(
        hashtextextended(NEW.student_id::text || ':' || NEW.semester_id::text, 0)
    );

    IF (SELECT count(*) FROM enrollments
        WHERE student_id = NEW.student_id AND semester_id = NEW.semester_id) >= 6 THEN
        RAISE EXCEPTION 'Students may enroll in at most six courses per semester'
            USING ERRCODE = 'check_violation';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER enrollments_max_six_courses
    BEFORE INSERT ON enrollments
    FOR EACH ROW
    EXECUTE FUNCTION enforce_max_six_courses();
