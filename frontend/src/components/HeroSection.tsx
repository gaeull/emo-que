import './HeroSection.css';

type Props = {
  onStart: () => void;
};

const HeroSection = ({ onStart }: Props) => {
  return (
    <section className="hero">
      <div>
        <p className="hero-kicker">AI-Based Personalized Emoticon Lab</p>
        <h1>Turn your vibe into a full emoticon pack.</h1>
        <p className="hero-sub">
          Emoque blends your ChatGPT history, survey insights, and 24 emotion layers to craft a bio
          plus export-ready emoticons in minutes.
        </p>
        <button className="primary" onClick={onStart}>
          Create My Emoticon
        </button>
      </div>
    </section>
  );
};

export default HeroSection;
