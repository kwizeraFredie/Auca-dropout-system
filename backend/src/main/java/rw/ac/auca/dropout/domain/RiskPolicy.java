package rw.ac.auca.dropout.domain;

import java.math.BigDecimal;

public final class RiskPolicy {
    public static final BigDecimal MINIMUM_ATTENDANCE_PERCENT = BigDecimal.valueOf(70);
    public static final BigDecimal PASSING_MARK_PERCENT = BigDecimal.valueOf(50);
    public static final int MAX_COURSES_PER_SEMESTER = 6;

    private RiskPolicy() {
    }

    public static boolean isAtRisk(BigDecimal attendancePercent, BigDecimal courseMark) {
        return attendancePercent != null && attendancePercent.compareTo(MINIMUM_ATTENDANCE_PERCENT) < 0
                || courseMark != null && courseMark.compareTo(PASSING_MARK_PERCENT) < 0;
    }
}
