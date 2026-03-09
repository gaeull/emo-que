import { useState } from 'react';
import './EmotionPicker.css';

const emotions = [
  'Happy',
  'Laughing',
  'Proud',
  'Love',
  'Excited',
  'Surprised',
  'Touched',
  'Shy',
  'Sad',
  'Crying',
  'Hurt',
  'Lonely',
  'Angry',
  'Annoyed',
  'Rage',
  'Jealous/Sulky',
  'Tired',
  'Burnout/Shocked',
  'Exhausted/Empty',
  'Sleepy',
  'Confused',
  'Thinking',
  'Skeptical',
  'Confident/Smug'
];

type Props = {
  defaultEmotions: string[];
  onSubmit: (emotions: string[]) => void;
};

const EmotionPicker = ({ defaultEmotions, onSubmit }: Props) => {
  const [selected, setSelected] = useState(defaultEmotions);

  const toggle = (emotion: string) => {
    const exists = selected.includes(emotion);
    if (exists) {
      setSelected(selected.filter((item) => item !== emotion));
    } else {
      setSelected([...selected, emotion]);
    }
  };

  return (
    <div className="emotion-picker">
      <p className="hint">Pick the moods you want to visualize (select multiple).</p>
      <div className="chip-grid">
        {emotions.map((emotion) => {
          const active = selected.includes(emotion);
          return (
            <button
              key={emotion}
              type="button"
              className={active ? 'chip active' : 'chip'}
              onClick={() => toggle(emotion)}
            >
              {emotion}
            </button>
          );
        })}
      </div>
      <button className="primary" onClick={() => onSubmit(selected)} disabled={!selected.length}>
        Launch Generation
      </button>
    </div>
  );
};

export default EmotionPicker;
