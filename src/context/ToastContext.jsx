import React, { createContext, useContext, useState, useCallback } from 'react';

const ToastContext = createContext();

export function useToast() {
  return useContext(ToastContext);
}

export function ToastProvider({ children }) {
  const [toast, setToast] = useState(null);

  const showToast = useCallback((message, type = 'info') => {
    setToast({ message, type });
    setTimeout(() => {
      setToast(null);
    }, 3000);
  }, []);

  const value = {
    showToast,
  };

  return (
    <ToastContext.Provider value={value}>
      {children}
      {toast && <Toast message={toast.message} type={toast.type} />}
    </ToastContext.Provider>
  );
}

function Toast({ message, type }) {
  const baseClasses = 'fixed bottom-20 left-1/2 -translate-x-1/2 px-6 py-3 rounded-full text-white text-sm shadow-lg transition-all duration-300';
  const typeClasses = {
    info: 'bg-gray-800',
    error: 'bg-red-500',
    success: 'bg-green-500',
  };

  return (
    <div className={`${baseClasses} ${typeClasses[type]}`}>
      {message}
    </div>
  );
}
