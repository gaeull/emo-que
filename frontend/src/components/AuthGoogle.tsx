import { useEffect, useState } from 'react';
import { getGoogleClientId, loginWithGoogle } from '../api/client';
import './AuthGoogle.css';

type Props = {
  onSuccess: (profile: { userId: string; name: string; email: string }) => void;
};

declare global {
  interface Window {
    google?: any;
  }
}

function decodeJwt(token: string): any {
  const payload = token.split('.')[1];
  const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
  return JSON.parse(decodeURIComponent(escape(json)));
}

const AuthGoogle = ({ onSuccess }: Props) => {
  const [clientId, setClientId] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;
    (async () => {
      try {
        const cid = await getGoogleClientId();
        if (!mounted) return;
        setClientId(cid);
      } catch (e) {
        console.error('Failed to get Google client id', e);
      }
    })();
    return () => {
      mounted = false;
    };
  }, []);

  useEffect(() => {
    if (!clientId) return;

    let cancelled = false;

    const ensureGoogleReadyAndRender = () => {
      if (cancelled) return;
      if (!window.google) {
        // Google script may not be loaded yet; retry shortly.
        setTimeout(ensureGoogleReadyAndRender, 200);
        return;
      }

      try {
        window.google.accounts.id.initialize({
          client_id: clientId,
          callback: async (resp: any) => {
            try {
              const jwt = resp.credential as string;
              const login = await loginWithGoogle(jwt);
              onSuccess({ userId: login.userId, name: login.name, email: login.email });
            } catch (e) {
              console.error('Login failed', e);
              alert('Google login failed.');
            }
          }
        });
        const div = document.getElementById('google-btn');
        if (div) {
          window.google.accounts.id.renderButton(div, { theme: 'outline', size: 'large' });
        }
      } catch (e) {
        console.error('Failed to initialize Google Identity Services', e);
      }
    };

    ensureGoogleReadyAndRender();

    return () => {
      cancelled = true;
    };
  }, [clientId, onSuccess]);

  const idMissing = !clientId;

  return (
    <section className="auth-google">
      <div className="auth-google__card">
        <header className="auth-google__brand">
          <span className="auth-google__badge">Emo-que</span>
          <h2>Sign in to craft your emoticon pack</h2>
          <p>
            Turn your vibe into a playful sticker set. We only use Google to verify
            your account.
          </p>
          <div className="auth-google__example">
            <div className="auth-google__example-grid" aria-label="Job-based emoticon previews">
              <figure className="auth-google__example-card">
                <div className="auth-google__example-frame" aria-hidden="true">
                  <img src="/Designer_sample_image.png" alt="Designer emoticon sample" />
                </div>
                <figcaption>Designer</figcaption>
              </figure>
              <figure className="auth-google__example-card">
                <div className="auth-google__example-frame" aria-hidden="true">
                  <img src="/Developer_sample_image.png" alt="Developer emoticon sample" />
                </div>
                <figcaption>Developer</figcaption>
              </figure>
              <figure className="auth-google__example-card">
                <div className="auth-google__example-frame" aria-hidden="true">
                  <img src="/Barista_simple_image.png" alt="Barista emoticon sample" />
                </div>
                <figcaption>Barista</figcaption>
              </figure>
            </div>
            <p className="auth-google__example-caption">
              Each emoticon is tailored to your job and personal vibe.
            </p>
          </div>
        </header>
        <div className="auth-google__panel">
          <div className="auth-google__panel-inner">
            <h3>What you get</h3>
            <ul>
              <li>Personalized character style matched to your profile.</li>
              <li>Emotion-ready set with clean, punchy line work.</li>
              <li>Downloadable PNG pack, ready for chat apps.</li>
            </ul>
          </div>
          <div className="auth-google__cta">
            <span className="auth-google__cta-label">Continue with Google</span>
            {idMissing ? (
              <div className="auth-google__loading">
                <span />
                Loading Google login…
              </div>
            ) : (
              <div id="google-btn" className="auth-google__button" />
            )}
            <p className="auth-google__note">No spam. No posting. Just your login.</p>
          </div>
        </div>
      </div>
      <div className="auth-google__ornament" aria-hidden="true">
        <span />
        <span />
        <span />
      </div>
    </section>
  );
};

export default AuthGoogle;
