package com.example.studyscoreai;

import android.content.Context;
import android.content.res.AssetFileDescriptor;

import org.tensorflow.lite.Interpreter;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * ScorePredictor wraps the TensorFlow Lite model "study_score.tflite".
 *
 * The model was trained in Python (see ml/train_and_convert.py) and has:
 *   - Input  tensor : float32, shape [1, 3]  -> {studyHours/10, attendance/100, previousMarks/100}
 *   - Output tensor : float32, shape [1, 1]  -> predictedMarks/100
 *
 * All ML related work (loading, normalising, inference, de-normalising) is kept in this one
 * class so that the Activity only deals with the user interface.
 */
public class ScorePredictor implements Closeable {

    /** File name of the model stored in app/src/main/assets/. */
    private static final String MODEL_FILE = "study_score.tflite";

    // Normalisation constants. They MUST be identical to the ones used during training in Python.
    public static final float MAX_HOURS = 10f;
    public static final float MAX_PERCENT = 100f;

    /** The TFLite interpreter that executes the model. */
    private final Interpreter interpreter;

    /**
     * Loads the model from assets and creates the Interpreter.
     *
     * @throws IOException if the model file is missing or cannot be memory-mapped.
     */
    public ScorePredictor(Context context) throws IOException {
        // Interpreter.Options lets us tune the runtime (threads, delegates ...).
        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(2); // the model is tiny, two CPU threads are plenty
        interpreter = new Interpreter(loadModelFile(context), options);
    }

    /**
     * Memory-maps the .tflite file from the assets folder (no copy into the Java heap).
     * This only works because the file is stored uncompressed (see noCompress in build.gradle).
     */
    private static MappedByteBuffer loadModelFile(Context context) throws IOException {
        try (AssetFileDescriptor fd = context.getAssets().openFd(MODEL_FILE);
             FileInputStream inputStream = new FileInputStream(fd.getFileDescriptor());
             FileChannel channel = inputStream.getChannel()) {
            long startOffset = fd.getStartOffset(); // where the model begins inside the APK
            long length = fd.getDeclaredLength();   // how many bytes it occupies
            return channel.map(FileChannel.MapMode.READ_ONLY, startOffset, length);
        }
    }

    /**
     * Runs one inference.
     *
     * @param studyHours    hours of study per day (0..10)
     * @param attendance    attendance percentage (0..100)
     * @param previousMarks marks in the previous exam (0..100)
     * @return predicted marks, clamped to 0..100
     */
    public float predict(float studyHours, float attendance, float previousMarks) {
        // 1) Prepare input: shape [1][3], values scaled to 0..1 exactly like in training.
        float[][] input = new float[1][3];
        input[0][0] = studyHours / MAX_HOURS;
        input[0][1] = attendance / MAX_PERCENT;
        input[0][2] = previousMarks / MAX_PERCENT;

        // 2) Allocate output buffer: shape [1][1].
        float[][] output = new float[1][1];

        // 3) Run inference. The Interpreter copies input -> model -> output.
        interpreter.run(input, output);

        // 4) Interpret output: scale back to 0..100 and clamp (a regression net may overshoot slightly).
        float marks = output[0][0] * MAX_PERCENT;
        return Math.max(0f, Math.min(100f, marks));
    }

    /** Releases native resources held by the Interpreter. Call from Activity.onDestroy(). */
    @Override
    public void close() {
        interpreter.close();
    }
}
