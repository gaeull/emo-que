import { FormEvent, useState } from 'react';
import type { SurveyForm } from '../api/client';
import './SurveyForm.css';

const personalityOptions = [
  'Creative',
  'Calm',
  'Bold',
  'Adventurous',
  'Empathetic',
  'Analytical',
  'Playful',
  'Minimal',
  'Romantic',
  'Nostalgic',
  'Techie',
  'Dreamer',
  'Leader',
  'Listener',
  'Optimistic',
  'Realist',
  'Mystic',
  'Chill',
  'Hyper',
  'Witty'
];

type Props = {
  defaultValue: SurveyForm;
  onSubmit: (payload: SurveyForm) => void;
};

const SurveyFormComponent = ({ defaultValue, onSubmit }: Props) => {
  const [form, setForm] = useState(defaultValue);

  const toggleKeyword = (keyword: string) => {
    const exists = form.personalityKeywords.includes(keyword);
    setForm({
      ...form,
      personalityKeywords: exists
        ? form.personalityKeywords.filter((item) => item !== keyword)
        : [...form.personalityKeywords, keyword].slice(-5)
    });
  };

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault();
    onSubmit(form);
  };

  return (
    <form className="survey" onSubmit={handleSubmit}>
      <div className="grid">
        <label>
          Name
          <input
            type="text"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            required
          />
        </label>
        <label>
          Email
          <input
            type="email"
            value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })}
            required
          />
        </label>
        <label>
          Gender
          <input
            type="text"
            value={form.gender}
            onChange={(e) => setForm({ ...form, gender: e.target.value })}
            required
          />
        </label>
        <label>
          Job / Occupation
          <input
            type="text"
            value={form.job}
            onChange={(e) => setForm({ ...form, job: e.target.value })}
            required
          />
        </label>
        <label>
          MBTI
          <input
            type="text"
            value={form.mbti}
            onChange={(e) => setForm({ ...form, mbti: e.target.value.toUpperCase() })}
            required
            maxLength={4}
          />
        </label>
        <label>
          Reference Emoticon URL (optional)
          <input
            type="url"
            value={form.sampleEmoticonUrls[0] ?? ''}
            onChange={(e) => setForm({ ...form, sampleEmoticonUrls: [e.target.value] })}
            placeholder="https://..."
          />
        </label>
      </div>

      <div>
        <p>Select up to 5 personality keywords:</p>
        <div className="chips">
          {personalityOptions.map((keyword) => {
            const active = form.personalityKeywords.includes(keyword);
            return (
              <button
                type="button"
                key={keyword}
                className={active ? 'chip active' : 'chip'}
                onClick={() => toggleKeyword(keyword)}
              >
                {keyword}
              </button>
            );
          })}
        </div>
      </div>

      <button className="primary" type="submit">
        Continue
      </button>
    </form>
  );
};

export default SurveyFormComponent;
