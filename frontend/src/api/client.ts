import axios from 'axios';

const api = axios.create({
  baseURL: '/api'
});

export interface SurveyForm {
  name: string;
  email: string;
  gender: string;
  job: string;
  mbti: string;
  personalityKeywords: string[];
  sampleEmoticonUrls: string[];
}

export interface GenerationPayload {
  userId: string;
  emotions: string[];
}

export const submitSurvey = async (payload: SurveyForm) => {
  const { data } = await api.post<{ userId: string }>('/survey', payload);
  return data;
};

export const importChat = async (payload: { userId: string; openAiApiKey: string; conversationId: string }) => {
  await api.post('/chat/import', payload);
};

export const createGenerationTask = async (payload: GenerationPayload) => {
  const { data } = await api.post<{ taskId: string; status: string }>(
    '/generation',
    payload
  );
  return data;
};

export const fetchGenerationResult = async (taskId: string) => {
  const { data } = await api.get<{
    taskId: string;
    status: string;
    bio: string;
    emotionImageUrls: Record<string, string>;
    downloadLinks: string[];
  }>(`/generation/${taskId}`);
  return data;
};

export const downloadZip = async (taskId: string, images: string[]) => {
  const { data } = await api.post<ArrayBuffer>(
    `/generation/${taskId}/download`,
    { images },
    { responseType: 'arraybuffer' }
  );
  return data;
};

export const getGoogleClientId = async () => {
  const { data } = await api.get<{ clientId: string }>(`/auth/google/client-id`);
  return data.clientId;
};

export const loginWithGoogle = async (idToken: string) => {
  const { data } = await api.post<{ userId: string; name: string; email: string }>(`/auth/google`, { idToken });
  return data;
};

export const initUser = async (payload: { name: string; email: string }) => {
  const { data } = await api.post<{ userId: string }>(`/users/init`, payload);
  return data;
};

export const uploadChatFile = async (userId: string, file: File) => {
  const fd = new FormData();
  fd.append('file', file);
  fd.append('userId', userId);
  const { data } = await api.post<{ taskId: string; status: string; failureReason?: string }>(
    `/chat/upload`,
    fd,
    { headers: { 'Content-Type': 'multipart/form-data' } }
  );
  return data;
};

export const createIntroTask = async (userId: string) => {
  const { data } = await api.post<{ taskId: string; status: string; failureReason?: string }>(`/bio`, { userId });
  return data;
};

export const fetchIntroTask = async (taskId: string) => {
  const { data } = await api.get<{ taskId: string; status: string; intro?: string; failureReason?: string }>(
    `/bio/${taskId}`
  );
  return data;
};
