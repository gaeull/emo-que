import { useMemo, useState } from 'react';
import { downloadZip } from '../api/client';
import './ResultGallery.css';

type Props = {
  taskId: string;
  bio: string;
  images: Record<string, string>;
  onRestart: () => void;
};

const ResultGallery = ({ taskId, bio, images, onRestart }: Props) => {
  const entries = useMemo(() => Object.entries(images ?? {}), [images]);
  const [downloading, setDownloading] = useState(false);
  const hasImages = entries.length > 0;

  const handleDownload = async () => {
    if (!taskId || !hasImages) return;
    try {
      setDownloading(true);
      const base64List = entries
        .map(([, value]) => (value.startsWith('data:') ? value.split(',')[1] ?? '' : value))
        .filter(Boolean);
      const zip = await downloadZip(taskId, base64List);
      const blob = new Blob([zip], { type: 'application/zip' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `emoticons-${taskId}.zip`;
      a.click();
      URL.revokeObjectURL(url);
    } catch (err) {
      console.error(err);
      alert('Download failed. Please try again.');
    } finally {
      setDownloading(false);
    }
  };

  return (
    <section className="results">
      <div className="result-card">
        <p className="bio-label">AI Bio</p>
        <h2>{bio || 'Generation still running...'}</h2>
        <div className="downloads">
          {hasImages ? (
            <button onClick={handleDownload} disabled={downloading}>
              {downloading ? 'Preparing zip...' : 'Download pack'}
            </button>
          ) : (
            <p>Images will appear once ready.</p>
          )}
        </div>
      </div>

      <div className="gallery">
        {entries.map(([emotion, data]) => {
          const src = data.startsWith('data:') ? data : `data:image/png;base64,${data}`;
          return (
            <figure key={emotion}>
              <img src={src} alt={emotion} />
              <figcaption>{emotion}</figcaption>
            </figure>
          );
        })}
      </div>

      <button className="secondary" onClick={onRestart}>
        Start another pack
      </button>
    </section>
  );
};

export default ResultGallery;
