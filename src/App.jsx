import React, { useEffect, useState } from 'react';
import { Routes, Route, Navigate, useNavigate, useLocation } from 'react-router-dom';
import { App as CapApp } from '@capacitor/app';
import { Capacitor } from '@capacitor/core';
import { supabase } from './lib/supabase';
import Layout from './components/layout/Layout';
import Dashboard from './components/views/Dashboard';
import UploadAnalysis from './components/views/UploadAnalysis';
import Patients from './components/views/Patients';
import Reports from './components/views/Reports';
import Settings from './components/views/Settings';
import TreatmentPlan from './components/views/TreatmentPlan';
import Splash from './components/onboarding/Splash';
import Welcome from './components/onboarding/Welcome';
import RoleSelection from './components/onboarding/RoleSelection';
import Auth from './components/onboarding/Auth';
import './App.css';

import Logo from './components/layout/Logo';

const DashboardLayout = ({ children }) => (
  <Layout>
    {children}
  </Layout>
);

function ProtectedRoute({ children }) {
  const [session, setSession] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Initialize Theme
    const savedTheme = localStorage.getItem('prophydent-theme') || 'light';
    document.documentElement.setAttribute('data-theme', savedTheme);

    supabase.auth.getSession().then(({ data: { session } }) => {
      setSession(session);
      setLoading(false);
    });

    const {
      data: { subscription },
    } = supabase.auth.onAuthStateChange((_event, session) => {
      setSession(session);
    });

    return () => subscription.unsubscribe();
  }, []);

  if (loading) {
    return (
      <div className="splash-screen">
        <div className="splash-content" style={{ position: 'fixed', top: '50%', left: '50%', transform: 'translate(-50%, -50%)', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', textAlign: 'center', width: '90%', maxWidth: '360px' }}>
          <Logo size={90} className="splash-logo" />
          <h1 className="splash-title" style={{ marginTop: '1.2rem', marginBottom: '1.2rem', fontSize: '1.8rem', color: 'var(--color-primary)' }}>Smile Guard AI</h1>
          <div className="loading-spinner"></div>
        </div>
      </div>
    );
  }

  if (!session) {
    return <Navigate to="/welcome" />;
  }

  return children;
}

function App() {
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    if (Capacitor.isNativePlatform()) {
      const backListener = CapApp.addListener('backButton', ({ canGoBack }) => {
        if (location.pathname === '/dashboard' || location.pathname === '/welcome' || location.pathname === '/') {
          CapApp.exitApp();
        } else if (canGoBack) {
          navigate(-1);
        } else {
          CapApp.exitApp();
        }
      });

      return () => {
        backListener.then(h => h.remove());
      };
    }
  }, [location, navigate]);

  return (
    <Routes>
      {/* Onboarding Routes */}
      <Route path="/" element={<Splash />} />
      <Route path="/welcome" element={<Welcome />} />
      <Route path="/role" element={<RoleSelection />} />
      <Route path="/auth" element={<Auth />} />

      {/* Main App Routes (Protected) */}
      <Route 
        path="/dashboard" 
        element={
          <ProtectedRoute>
            <DashboardLayout>
              <Dashboard onNavigate={(path) => navigate(`/${path}`)} />
            </DashboardLayout>
          </ProtectedRoute>
        } 
      />
      <Route 
        path="/patients" 
        element={
          <ProtectedRoute>
            <DashboardLayout>
              <Patients />
            </DashboardLayout>
          </ProtectedRoute>
        } 
      />
      <Route 
        path="/analysis" 
        element={
          <ProtectedRoute>
            <DashboardLayout>
              <UploadAnalysis onNavigate={(path) => navigate(`/${path}`)} />
            </DashboardLayout>
          </ProtectedRoute>
        } 
      />
      <Route 
        path="/reports" 
        element={
          <ProtectedRoute>
            <DashboardLayout>
              <Reports />
            </DashboardLayout>
          </ProtectedRoute>
        } 
      />
      <Route 
        path="/settings" 
        element={
          <ProtectedRoute>
            <DashboardLayout>
              <Settings />
            </DashboardLayout>
          </ProtectedRoute>
        } 
      />
      <Route 
        path="/treatment" 
        element={
          <ProtectedRoute>
            <DashboardLayout>
              <TreatmentPlan />
            </DashboardLayout>
          </ProtectedRoute>
        } 
      />

      {/* Catch-all */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default App;
