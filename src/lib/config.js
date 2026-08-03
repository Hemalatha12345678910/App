import { Capacitor } from '@capacitor/core';

const DEFAULT_PRODUCTION_URL = import.meta.env.VITE_BACKEND_URL || 'https://pdd-backend-os1e.onrender.com/analyze';
const KEY_BACKEND_URL = 'prophydent-backend-url';

export const getBackendUrl = () => {
  const savedUrl = localStorage.getItem(KEY_BACKEND_URL);
  if (savedUrl) {
    if (savedUrl.includes('pdd-iawo.onrender.com') || savedUrl.includes('10.0.2.2')) {
      localStorage.removeItem(KEY_BACKEND_URL);
    } else {
      return resolveUrlForPlatform(savedUrl);
    }
  }

  // Always connect to the live production backend URL on mobile and web
  return DEFAULT_PRODUCTION_URL;
};

export const setBackendUrl = (url) => {
  if (!url) {
    localStorage.removeItem(KEY_BACKEND_URL);
  } else {
    localStorage.setItem(KEY_BACKEND_URL, url.trim());
  }
};

export const resetBackendUrl = () => {
  localStorage.removeItem(KEY_BACKEND_URL);
};

export const resolveUrlForPlatform = (url) => {
  if (!url) return DEFAULT_PRODUCTION_URL;
  return url;
};
