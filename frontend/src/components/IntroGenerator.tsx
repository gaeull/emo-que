import { useEffect, useState } from 'react';
import { fetchIntroTask, uploadChatFile } from '../api/client';

type Props = {
  userId: string;
  onIntro: (intro: string) => void;
};

const IntroGenerator = ({ userId, onIntro }: Props) => {
  const [file, setFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [taskId, setTaskId] = useState<string | null>(null);

  const handleRun = async () => {
    if (!userId) return;
    if (!file) {
      setError('Please upload a conversation history file (.txt).');
      return;
    }
    try {
      setLoading(true);
      setError(null);
      const task = await uploadChatFile(userId, file);
      setTaskId(task.taskId);
    } catch (e) {
      console.error(e);
      setError('Failed to generate intro.');
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!loading || !taskId) return;
    const interval = setInterval(async () => {
      try {
        const task = await fetchIntroTask(taskId);
        if (task.status === 'COMPLETED') {
          setLoading(false);
          setTaskId(null);
          onIntro(task.intro ?? '');
        } else if (task.status === 'FAILED') {
          setLoading(false);
          setTaskId(null);
          setError('Failed to generate intro.');
        }
      } catch (e) {
        console.error(e);
        setLoading(false);
        setTaskId(null);
        setError('Failed to generate intro.');
      }
    }, 2500);

    return () => clearInterval(interval);
  }, [loading, taskId, onIntro]);

  return (
    <div style={{ border: '1px solid #e5e7eb', padding: '1rem', borderRadius: '0.75rem' }}>
      <h3>Generate a one-line introduction</h3>
      <p>Upload a chat history file (.txt / .json / .html). We’ll craft your intro with Llama 3 8B.</p>
      <div style={{ display: 'grid', gap: '0.5rem', alignItems: 'center' }}>
        <input type="file" accept=".txt,.json,.html" onChange={(e) => setFile(e.target.files?.[0] ?? null)} />
        <div style={{ marginTop: '0.5rem', display: 'flex', gap: '0.75rem', alignItems: 'center' }}>
          <button className="primary" onClick={handleRun} disabled={loading}>
            {loading ? 'Generating...' : 'Generate Intro'}
          </button>
          <span style={{ color: '#6b7280', fontSize: '0.9rem' }}>Powered by Llama 3 8B.</span>
        </div>
      </div>
      {error && <p style={{ color: '#b91c1c' }}>{error}</p>}
    </div>
  );
};

export default IntroGenerator;
