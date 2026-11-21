# StudyScore AI - On-Device Exam Score Predictor (TensorFlow Lite)

Assignment No. 2 - AI-Based Mini Application Development: Integrating ML/DL Models into Android with TensorFlow Lite.

**Student:** [Your Name] | **Roll No:** [Your Roll Number] | **Course:** Mobile Application Development

## What it does
StudyScore AI predicts a student's expected exam marks (0-100) from three inputs:
study hours per day, attendance %, and previous exam marks. It also shows a letter grade and advice.
The prediction runs **fully offline on the phone** with a TensorFlow Lite model (`study_score.tflite`).

> The model is trained on **synthetic data** (marks = 0.5*previous + 3*hours + 0.2*attendance + noise).
> It is an educational demo, not a validated academic predictor.

## Reference repository
Based on / inspired by: https://github.com/Uzair390-Del/NumberGuessingAppTflite.git
(single-input linear model `linear.tflite`; its model-loading approach `MappedByteBuffer` + `Interpreter.run()` is reused.)

## Project structure
```
StudyScoreAI/
  app/
    build.gradle                       module config + TFLite dependency
    src/main/assets/study_score.tflite the model (input [1,3] float32, output [1,1] float32)
    src/main/java/com/example/studyscoreai/
        MainActivity.java              UI, validation, shows results
        ScorePredictor.java            loads model, normalises, runs inference
        GradeHelper.java               grade + advice (unit tested)
    src/main/res/                      layout, strings, colors, theme, icons
    src/test/.../GradeHelperTest.java  JUnit tests
  ml/
    train_and_convert.py               trains the Keras model and converts it to .tflite
    results.json, loss_curve.png       evaluation output
  build.gradle, settings.gradle, gradle/, gradlew
```

## Prerequisites
- Android Studio (Ladybug or newer; AGP 8.9.1 needs a recent version) with Android SDK 35
- JDK 17 (bundled with Android Studio)
- Android emulator (API 24+) or a physical Android phone with USB debugging
- Internet only for the first Gradle sync (to download dependencies)
- *(Optional, only to retrain the model)* Python 3.10+ with `pip install tensorflow numpy matplotlib`

## Install and run
1. Clone: `git clone https://github.com/[your-username]/StudyScoreAI.git` (or unzip `StudyScoreAI.zip`).
2. Android Studio -> **File > Open** -> select the `StudyScoreAI` folder.
3. Wait for **Gradle Sync** to finish.
4. Choose an emulator/device and press **Run** (green triangle).
5. Enter e.g. Hours = 5, Attendance = 75, Previous = 60 and press **Predict** (expected about 60 marks).

Command line alternative: `./gradlew assembleDebug` -> APK in `app/build/outputs/apk/debug/`.
Unit tests: `./gradlew test`.

## Retrain / reconvert the model (optional)
```
cd ml
python train_and_convert.py        # creates study_score.tflite
cp study_score.tflite ../app/src/main/assets/
```
Keep the normalisation constants (hours/10, attendance/100, previous/100, output*100) identical in
`train_and_convert.py` and `ScorePredictor.java`.

## Dependencies
| Library | Version |
|---|---|
| org.tensorflow:tensorflow-lite | 2.14.0 |
| androidx.appcompat | 1.7.0 |
| com.google.android.material | 1.12.0 |
| androidx.constraintlayout | 2.2.1 |
| junit (test) | 4.13.2 |

## Model results
Test MAE = 1.58 marks on 1200 held-out samples; model size = 2804 bytes.

## Acknowledgements
Original repository https://github.com/Uzair390-Del/NumberGuessingAppTflite.git; TensorFlow / TensorFlow Lite (Apache 2.0); Material Components for Android.
