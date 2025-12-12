import React from 'react';

function LoadingOverlay() {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-white">
      <div className="flex flex-col items-center gap-4 rounded-3xl border border-gray-100 bg-white/90 px-10 py-8 shadow-2xl">
        <div className="h-10 w-10 animate-spin rounded-full border-2 border-[#4A9782] border-t-transparent" />
        <p className="text-xs font-semibold uppercase tracking-[0.3em] text-gray-500">Loading</p>
      </div>
    </div>
  )
}

export default LoadingOverlay;
