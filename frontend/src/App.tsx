import { useEffect, useMemo, useState } from 'react';
import HeroSection from './components/HeroSection';
import SurveyForm from './components/SurveyForm';
import AuthGoogle from './components/AuthGoogle';
import IntroGenerator from './components/IntroGenerator';
import EmotionPicker from './components/EmotionPicker';
import LoadingScreen from './components/LoadingScreen';
import ResultGallery from './components/ResultGallery';
import {
  SurveyForm as SurveyPayload,
  submitSurvey,
  createGenerationTask,
  fetchGenerationResult
} from './api/client';

const defaultSurvey: SurveyPayload = {
  name: '',
  email: '',
  gender: '',
  job: '',
  mbti: 'ENFP',
  personalityKeywords: ['Creative'],
  sampleEmoticonUrls: ['']
};

type View = 'hero' | 'auth' | 'survey' | 'emotions' | 'loading' | 'result';

const App = () => {
  const [view, setView] = useState<View>('hero');
  const [survey, setSurvey] = useState(defaultSurvey);
  const [userId, setUserId] = useState<string | null>(null);
  const [intro, setIntro] = useState<string | null>(null);
  const [taskId, setTaskId] = useState<string | null>(null);
  const [result, setResult] = useState<{
    bio: string;
    images: Record<string, string>;
  } | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleSurveySubmit = async (payload: SurveyPayload) => {
    try {
      const { userId } = await submitSurvey(payload);
      setSurvey(payload);
      setUserId(userId);
      setView('emotions');
      setError(null);
    } catch (err) {
      console.error(err);
      setError('Unable to save your profile. Please try again.');
    }
  };

  const handleEmotionSubmit = async (emotions: string[]) => {
    if (!userId) return;
    try {
      const { taskId } = await createGenerationTask({ userId, emotions });
      setTaskId(taskId);
      setResult(null);
      setView('loading');
      setError(null);
    } catch (err) {
      console.error(err);
      setError('Failed to enqueue generation.');
    }
  };

  useEffect(() => {
    if (view !== 'loading' || !taskId) return;
    const interval = setInterval(async () => {
      const response = await fetchGenerationResult(taskId);
      if (response.status === 'COMPLETED' || response.status === 'FAILED') {
        clearInterval(interval);
        if (response.status === 'COMPLETED') {
          setResult({
            bio: response.bio,
            images: response.emotionImageUrls
          });
          setView('result');
        } else {
          setError('Generation failed. Please retry.');
          setView('emotions');
        }
      }
    }, 3000);

    return () => clearInterval(interval);
  }, [view, taskId]);

  const content = useMemo(() => {
    switch (view) {
      case 'hero':
        return <HeroSection onStart={() => setView('auth')} />;
      case 'auth':
        return (
          <div style={{ display: 'grid', gap: '1.25rem' }}>
            <AuthGoogle
              onSuccess={({ userId, name, email }) => {
                setSurvey((s) => ({ ...s, name, email }));
                setUserId(userId);
                setView('survey');
              }}
            />
          </div>
        );
      case 'survey':
        return (
          <div style={{ display: 'grid', gap: '1.25rem' }}>
            {userId && (
              <IntroGenerator
                userId={userId}
                onIntro={(text) => {
                  setIntro(text);
                }}
              />
            )}
            {intro && (
              <div style={{ background: '#eef2ff', padding: '0.75rem 1rem', borderRadius: '0.75rem' }}>
                <strong>One-line intro:</strong> {intro}
              </div>
            )}
            <SurveyForm defaultValue={survey} onSubmit={handleSurveySubmit} />
          </div>
        );
      case 'emotions':
        return <EmotionPicker defaultEmotions={['Happy', 'Confident/Smug']} onSubmit={handleEmotionSubmit} />;
      case 'loading':
        return <LoadingScreen message="Analyzing vibe and painting emoticons..." />;
      case 'result':
        return (
          <ResultGallery
            taskId={taskId ?? ''}
            bio={result?.bio ?? ''}
            images={result?.images ?? {}}
            onRestart={() => {
              setView('hero');
              setResult(null);
              setSurvey(defaultSurvey);
              setUserId(null);
              setTaskId(null);
            }}
          />
        );
      default:
        return null;
    }
  }, [view, survey, result]);

  return (
    <main style={{ maxWidth: '960px', margin: '0 auto', padding: '2rem 1rem' }}>
      {error && (
        <div
          style={{
            background: '#fee2e2',
            color: '#991b1b',
            padding: '0.75rem 1rem',
            borderRadius: '0.75rem',
            marginBottom: '1.5rem'
          }}
        >
          {error}
        </div>
      )}
      {content}
    </main>
  );
};

export default App;
