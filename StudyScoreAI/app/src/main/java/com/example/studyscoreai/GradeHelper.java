package com.example.studyscoreai;

/**
 * Pure-Java helper (no Android classes) that converts the predicted marks into a letter grade
 * and a short piece of advice. Kept separate so it can be unit-tested on the JVM.
 */
public final class GradeHelper {

    private GradeHelper() { }

    /** Returns the letter grade for the given marks (0..100). */
    public static String grade(float marks) {
        if (marks >= 85f) return "A";
        if (marks >= 75f) return "B";
        if (marks >= 65f) return "C";
        if (marks >= 50f) return "D";
        return "F";
    }

    /** Returns a short motivational message for the given marks. */
    public static String advice(float marks) {
        if (marks >= 85f) return "Excellent! Keep up your current routine.";
        if (marks >= 75f) return "Very good. A little more revision can push you to an A.";
        if (marks >= 65f) return "Good. Try increasing daily study hours and attendance.";
        if (marks >= 50f) return "Passing, but there is room to improve. Study more regularly.";
        return "At risk. Increase study time and attend classes regularly.";
    }

    /** Returns true if value lies inside [min, max]. */
    public static boolean inRange(float value, float min, float max) {
        return value >= min && value <= max;
    }
}
