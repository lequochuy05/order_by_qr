import * as tf from '@tensorflow/tfjs';

let model = null;
let labels = null;
let isLoading = false;

const MODEL_PATH = '/models/dish-classifier/model.json';
const LABELS_PATH = '/models/dish-classifier/labels.json';
const INPUT_SIZE = 224;

export const aiLocalService = {

  isModelLoaded: () => model !== null,

  loadModel: async () => {
    if (model) return;
    if (isLoading) return;

    isLoading = true;
    try {
      model = await tf.loadGraphModel(MODEL_PATH);
      const res = await fetch(LABELS_PATH);
      labels = await res.json();

      // Warm up: chạy 1 prediction giả để khởi tạo GPU/WebGL
      const warmup = tf.zeros([1, INPUT_SIZE, INPUT_SIZE, 3]);
      await model.predict(warmup).data();
      warmup.dispose();
    } catch (err) {
      model = null;
      labels = null;
      throw new Error('Không thể tải model AI: ' + err.message);
    } finally {
      isLoading = false;
    }
  },

  analyzeDish: async (imageSource) => {
    await aiLocalService.loadModel();

    // Tạo Image element nếu nhận URL string
    let imgElement;
    if (typeof imageSource === 'string') {
      imgElement = new Image();
      imgElement.crossOrigin = 'anonymous';
      imgElement.src = imageSource;
      await new Promise((resolve, reject) => {
        imgElement.onload = resolve;
        imgElement.onerror = () => reject(new Error('Không thể tải ảnh'));
      });
    } else {
      imgElement = imageSource;
    }

    // Preprocessing: resize 224x224, normalize 0-1
    const tensor = tf.tidy(() => {
      return tf.browser.fromPixels(imgElement)
        .resizeBilinear([INPUT_SIZE, INPUT_SIZE])
        .toFloat()
        .div(255.0)
        .expandDims(0); // [1, 224, 224, 3]
    });

    // Predict
    const predictions = await model.predict(tensor).data();
    tensor.dispose();

    // Tìm class có xác suất cao nhất
    const probs = Array.from(predictions);
    const maxProb = Math.max(...probs);
    const maxIndex = probs.indexOf(maxProb);

    // Top 3 predictions
    const top3 = probs
      .map((p, i) => ({ index: i, label: labels[String(i)] || `Class ${i}`, confidence: p }))
      .sort((a, b) => b.confidence - a.confidence)
      .slice(0, 3);

    return {
      name: labels[String(maxIndex)] || `Unknown`,
      confidence: maxProb,
      classId: maxIndex,
      top3,
    };
  },

  dispose: () => {
    if (model) {
      model.dispose();
      model = null;
      labels = null;
    }
  }
};
