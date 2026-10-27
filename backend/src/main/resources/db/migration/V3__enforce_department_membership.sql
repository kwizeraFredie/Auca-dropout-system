ALTER TABLE users
    ADD CONSTRAINT users_department_required_for_non_admin
    CHECK (role = 'ADMIN' OR department IS NOT NULL);
