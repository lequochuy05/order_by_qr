import api from '../api';

export const aiService = {
  analyzeDish: async (file) => {
    const formData = new FormData();
    formData.append('file', file);

    const response = await api.post('/admin/ai/analyze-dish', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });

    // The backend now parses the Gemini response and returns either:
    // 1. The inner JSON object (dish details)
    // 2. An error object { error: "..." }
    let data = response.data;

    // If the backend returned a string (e.g. if content-type wasn't application/json), try to parse it
    if (typeof data === 'string') {
      try {
        data = JSON.parse(data);
      } catch {
        // console.error("Error parsing AI response", e, data);
        return { error: "Không thể đọc được kết quả từ AI. Vui lòng thử lại." };
      }
    }

    return data;
  }
};
