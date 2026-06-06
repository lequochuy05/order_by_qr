let tf = null;
let model = null;
let labels = null;
let isLoading = false;

const MODEL_PATH = '/models/dish-classifier/model.json';
const LABELS_PATH = '/models/dish-classifier/labels.json';
const INPUT_SIZE = 224;

const getTensorFlow = async () => {
  if (!tf) {
    tf = await import('@tensorflow/tfjs');
  }
  return tf;
};

export const aiLocalService = {
  isModelLoaded: () => model !== null,

  loadModel: async () => {
    if (model) return;
    if (isLoading) return;

    isLoading = true;
    try {
      const tfRuntime = await getTensorFlow();
      model = await tfRuntime.loadGraphModel(MODEL_PATH);
      const res = await fetch(LABELS_PATH);
      labels = await res.json();

      const warmup = tfRuntime.zeros([1, INPUT_SIZE, INPUT_SIZE, 3]);
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
    const tfRuntime = await getTensorFlow();

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

    const tensor = tfRuntime.tidy(() => {
      return tfRuntime.browser.fromPixels(imgElement)
        .resizeBilinear([INPUT_SIZE, INPUT_SIZE])
        .toFloat()
        .div(255.0)
        .expandDims(0);
    });

    const predictions = await model.predict(tensor).data();
    tensor.dispose();

    const probs = Array.from(predictions);
    const maxProb = Math.max(...probs);
    const maxIndex = probs.indexOf(maxProb);

    const top3 = probs
      .map((p, i) => {
        const data = labels[String(i)];
        return {
          index: i,
          label: data?.name || `Class ${i}`,
          confidence: p
        };
      })
      .sort((a, b) => b.confidence - a.confidence)
      .slice(0, 3);

    const dishData = labels[String(maxIndex)];

    return {
      name: dishData?.name || dishData || "NaN",
      categoryId: dishData?.categoryid || null,
      price: dishData?.price || null,
      confidence: maxProb,
      classId: maxIndex,
      top3 // Bắt buộc phải có cái này
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
