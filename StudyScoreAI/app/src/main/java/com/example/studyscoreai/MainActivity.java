package com.example.studyscoreai;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;
import java.util.Locale;

/**
 * Main (and only) screen of StudyScore AI.
 * Flow: read inputs -> validate -> ScorePredictor.predict() (TFLite) -> show score, grade, advice.
 */
public class MainActivity extends AppCompatActivity {

    // UI widgets
    private TextInputLayout layoutHours, layoutAttendance, layoutPrevious;
    private TextInputEditText etHours, etAttendance, etPrevious;
    private TextView tvScore, tvGrade, tvAdvice, tvModelStatus;
    private Button btnPredict, btnReset;

    /** Wrapper around the TFLite model; null if the model failed to load. */
    private ScorePredictor predictor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindViews();

        // Load the model ONCE here (loading is expensive), not on every button click.
        try {
            predictor = new ScorePredictor(this);
            tvModelStatus.setText(R.string.model_ok);
        } catch (IOException e) {
            predictor = null;
            tvModelStatus.setText(R.string.model_fail);
            btnPredict.setEnabled(false);
        }

        btnPredict.setOnClickListener(v -> onPredictClicked());
        btnReset.setOnClickListener(v -> resetAll());
    }

    /** Finds every view from the XML layout. */
    private void bindViews() {
        layoutHours = findViewById(R.id.layoutHours);
        layoutAttendance = findViewById(R.id.layoutAttendance);
        layoutPrevious = findViewById(R.id.layoutPrevious);
        etHours = findViewById(R.id.etHours);
        etAttendance = findViewById(R.id.etAttendance);
        etPrevious = findViewById(R.id.etPrevious);
        tvScore = findViewById(R.id.tvScore);
        tvGrade = findViewById(R.id.tvGrade);
        tvAdvice = findViewById(R.id.tvAdvice);
        tvModelStatus = findViewById(R.id.tvModelStatus);
        btnPredict = findViewById(R.id.btnPredict);
        btnReset = findViewById(R.id.btnReset);
    }

    /** Handles the Predict button: validate, run the model, display the result. */
    private void onPredictClicked() {
        Float hours = readValue(etHours, layoutHours, 0, 10);
        Float attendance = readValue(etAttendance, layoutAttendance, 0, 100);
        Float previous = readValue(etPrevious, layoutPrevious, 0, 100);
        if (hours == null || attendance == null || previous == null || predictor == null) {
            return; // an error message is already shown under the invalid field
        }

        float marks = predictor.predict(hours, attendance, previous); // <-- TFLite inference

        String grade = GradeHelper.grade(marks);
        tvScore.setText(String.format(Locale.US, "Predicted marks: %.1f / 100", marks));
        tvGrade.setText("Grade: " + grade);
        tvGrade.setTextColor(colorForMarks(marks));
        tvAdvice.setText(GradeHelper.advice(marks));
    }

    /**
     * Reads a number from the field and validates it.
     * @return the value, or null (after showing an error) if empty / out of range.
     */
    private Float readValue(TextInputEditText field, TextInputLayout layout, int min, int max) {
        layout.setError(null);
        String text = field.getText() == null ? "" : field.getText().toString().trim();
        if (text.isEmpty()) {
            layout.setError(getString(R.string.err_empty));
            return null;
        }
        try {
            float value = Float.parseFloat(text);
            if (!GradeHelper.inRange(value, min, max)) {
                layout.setError(getString(R.string.err_range, min, max));
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            layout.setError(getString(R.string.err_empty));
            return null;
        }
    }

    /** Green for good marks, orange for average, red for weak. */
    private int colorForMarks(float marks) {
        if (marks >= 75f) return getColor(R.color.good);
        if (marks >= 50f) return getColor(R.color.warn);
        return getColor(R.color.bad);
    }

    /** Clears inputs, errors and the result card. */
    private void resetAll() {
        etHours.setText("");
        etAttendance.setText("");
        etPrevious.setText("");
        layoutHours.setError(null);
        layoutAttendance.setError(null);
        layoutPrevious.setError(null);
        tvScore.setText(R.string.result_placeholder);
        tvGrade.setText("");
        tvGrade.setTextColor(Color.BLACK);
        tvAdvice.setText("");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (predictor != null) {
            predictor.close(); // free native memory
        }
    }
}
