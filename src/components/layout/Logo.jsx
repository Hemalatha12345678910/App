import React from 'react';
import logoImg from '../../assets/logo.png';

export default function Logo({ className = "logo-img", size = 40 }) {
  return (
    <img 
      src={logoImg} 
      alt="Smile Guard AI" 
      className={className} 
      style={{ 
        width: size, 
        height: size, 
        objectFit: 'contain',
        borderRadius: '50%',
        boxShadow: '0 4px 14px rgba(0, 75, 135, 0.25)'
      }} 
    />
  );
}
