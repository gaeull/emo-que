import './LoadingScreen.css';

type Props = {
  message: string;
};

const LoadingScreen = ({ message }: Props) => {
  return (
    <div className="loading">
      <div className="spinner" />
      <p>{message}</p>
    </div>
  );
};

export default LoadingScreen;
