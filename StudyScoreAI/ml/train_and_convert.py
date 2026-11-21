"""
StudyScore AI - train a small regression network and convert it to TFLite.
Inputs (normalised to 0..1): [study_hours/10, attendance/100, previous_marks/100]
Output (0..1): predicted_marks/100
"""
import json, numpy as np, tensorflow as tf
import matplotlib; matplotlib.use("Agg"); import matplotlib.pyplot as plt

np.random.seed(42); tf.random.set_seed(42)
N = 6000
hours = np.random.uniform(0, 10, N)
att   = np.random.uniform(40, 100, N)
prev  = np.random.uniform(20, 100, N)
# Synthetic "ground truth" used only for demonstration (documented as such in the report)
score = 0.50*prev + 3.0*hours + 0.20*att + np.random.normal(0, 2.0, N)
score = np.clip(score, 0, 100)

X = np.stack([hours/10.0, att/100.0, prev/100.0], axis=1).astype("float32")
y = (score/100.0).astype("float32").reshape(-1, 1)
idx = np.random.permutation(N); s = int(N*0.8)
Xtr, ytr, Xte, yte = X[idx[:s]], y[idx[:s]], X[idx[s:]], y[idx[s:]]

model = tf.keras.Sequential([
    tf.keras.layers.Input(shape=(3,)),
    tf.keras.layers.Dense(16, activation="relu"),
    tf.keras.layers.Dense(8, activation="relu"),
    tf.keras.layers.Dense(1),
])
model.compile(optimizer=tf.keras.optimizers.Adam(0.01), loss="mse", metrics=["mae"])
hist = model.fit(Xtr, ytr, epochs=60, batch_size=64, validation_split=0.1, verbose=0)
loss, mae = model.evaluate(Xte, yte, verbose=0)
print("Test MSE", loss, "Test MAE (marks)", mae*100)

# ---- Convert to TFLite ----
conv = tf.lite.TFLiteConverter.from_keras_model(model)
tflite = conv.convert()
open("study_score.tflite", "wb").write(tflite)
print("tflite size bytes:", len(tflite))
model.save("study_score_model.keras")

# ---- Verify with the TFLite interpreter (same call sequence as Android) ----
it = tf.lite.Interpreter(model_content=tflite); it.allocate_tensors()
inp, out = it.get_input_details()[0], it.get_output_details()[0]
print("input", inp["shape"], inp["dtype"], "output", out["shape"], out["dtype"])
cases = [(2,60,50),(5,75,60),(8,90,80),(1,50,40),(10,100,100),(0,40,20),(6,85,70),(4,70,55)]
rows=[]
for h,a,p in cases:
    x = np.array([[h/10.0, a/100.0, p/100.0]], dtype="float32")
    it.set_tensor(inp["index"], x); it.invoke()
    pred = float(it.get_tensor(out["index"])[0][0])*100
    pred = max(0,min(100,pred))
    truth = max(0,min(100,0.5*p+3*h+0.2*a))
    rows.append(dict(hours=h,attendance=a,previous=p,predicted=round(pred,2),reference=round(truth,2)))
    print(rows[-1])
json.dump(dict(mae=float(mae*100), mse=float(loss), size=len(tflite), cases=rows,
               epochs=60, train=s, test=N-s), open("results.json","w"), indent=1)

plt.figure(figsize=(6,3.4)); plt.plot(hist.history["loss"],label="train loss"); plt.plot(hist.history["val_loss"],label="validation loss")
plt.yscale("log"); plt.xlabel("Epoch"); plt.ylabel("MSE loss (log)"); plt.legend(); plt.grid(alpha=.3); plt.tight_layout(); plt.savefig("loss_curve.png", dpi=160)
