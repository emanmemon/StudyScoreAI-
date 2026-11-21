package com.example.studyscoreai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** JVM unit tests for the pure-Java grading logic. */
public class GradeHelperTest {
    @Test public void gradeBoundaries() {
        assertEquals("A", GradeHelper.grade(85f));
        assertEquals("B", GradeHelper.grade(75f));
        assertEquals("C", GradeHelper.grade(65f));
        assertEquals("D", GradeHelper.grade(50f));
        assertEquals("F", GradeHelper.grade(49.9f));
    }
    @Test public void rangeCheck() {
        assertTrue(GradeHelper.inRange(0, 0, 10));
        assertTrue(GradeHelper.inRange(10, 0, 10));
        assertFalse(GradeHelper.inRange(10.1f, 0, 10));
        assertFalse(GradeHelper.inRange(-1, 0, 10));
    }
}
