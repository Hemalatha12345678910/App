import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import Logo from '../layout/Logo';
import './Onboarding.css';

export default function Splash() {
  const navigate = useNavigate();

  useEffect(() => {
    const timer = setTimeout(() => {
      navigate('/welcome');
    }, 2500);
    return () => clearTimeout(timer);
  }, [navigate]);

  return (
    <div className="splash-screen">
      <div className="splash-content" style={{ position: 'fixed', top: '50%', left: '50%', transform: 'translate(-50%, -50%)', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', textAlign: 'center', width: '90%', maxWidth: '360px' }}>
        <Logo size={100} className="splash-logo" />
        <h1 className="splash-title" style={{ marginTop: '1.2rem', marginBottom: '1.2rem', fontSize: '2rem', color: 'var(--color-primary)' }}>Smile Guard AI</h1>
        <div className="loading-spinner"></div>
      </div>
    </div>
  );
}
