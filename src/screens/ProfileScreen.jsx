import React, { useState, useMemo, useEffect } from 'react';
import { getTodayDateString } from '../utils';
import { useTasks } from '../context/TaskContext';

function ProfileScreen() {
  const tasks = useTasks();
  const [showQna, setShowQna] = useState(false)
  const [qnaAnswers, setQnaAnswers] = useState({
    completed: '',
    mistake: '',
    summary: ''
  })

  const totalTasks = tasks.length
  const totalHours = useMemo(() => {
    return tasks.reduce((total, task) => {
      const [startHour, startMin] = task.startTime.split(':').map(Number)
      const [endHour, endMin] = task.endTime.split(':').map(Number)
      const startMinutes = startHour * 60 + startMin
      const endMinutes = endHour * 60 + endMin
      return total + (endMinutes - startMinutes) / 60
    }, 0)
  }, [tasks])

  useEffect(() => {
    const key = `timeProtectorQna-${getTodayDateString()}`
    const saved = localStorage.getItem(key)
    if (saved) {
      try {
        const parsed = JSON.parse(saved)
        setQnaAnswers({
          completed: parsed.completed || '',
          mistake: parsed.mistake || '',
          summary: parsed.summary || ''
        })
      } catch {
        // ignore parse errors
      }
    }
  }, [])

  const handleQnaChange = (e) => {
    const { name, value } = e.target
    setQnaAnswers(prev => ({ ...prev, [name]: value }))
  }

  const handleSaveQna = () => {
    const key = `timeProtectorQna-${getTodayDateString()}`
    localStorage.setItem(key, JSON.stringify(qnaAnswers))
    setShowQna(false)
  }

  return (
    <div className="px-5 pt-5 space-y-6">
      <div className="flex flex-col items-center py-6">
        <div className="mb-4 flex h-24 w-24 items-center justify-center rounded-full bg-[#4A9782]/10 shadow-inner">
          <svg className="h-12 w-12 text-[#4A9782]" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z" clipRule="evenodd" />
          </svg>
        </div>
        <h2 className="text-xl font-bold text-[#1f352e]">Maruf Islam</h2>
        <p className="mt-1 text-sm text-gray-500">Learning AI & ML</p>
      </div>

      <div className="rounded-3xl border border-[#4A9782]/20 bg-gradient-to-r from-[#4A9782]/10 to-[#4A9782]/5 p-5">
        <div className="grid grid-cols-2 gap-4">
          <div className="text-center">
            <div className="text-2xl font-bold text-[#1f352e]">{totalTasks}</div>
            <div className="mt-1 text-xs uppercase tracking-[0.2em] text-gray-500">Total Blocks</div>
          </div>
          <div className="text-center">
            <div className="text-2xl font-bold text-[#1f352e]">{totalHours.toFixed(1)}h</div>
            <div className="mt-1 text-xs uppercase tracking-[0.2em] text-gray-500">Total Time</div>
          </div>
        </div>
      </div>

      <div className="space-y-3">
        <div className="flex items-center space-x-3 rounded-2xl border border-blue-100 bg-blue-50/70 p-3">
          <svg className="h-5 w-5 flex-shrink-0 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
          </svg>
          <span className="text-sm font-medium text-[#1f352e]">AI For Management</span>
        </div>
        <div className="flex items-center space-x-3 rounded-2xl border border-yellow-100 bg-yellow-50/70 p-3">
          <svg className="h-5 w-5 flex-shrink-0 text-yellow-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <span className="text-sm font-medium text-[#1f352e]">Manage Your Time</span>
        </div>
        <div className="flex items-center space-x-3 rounded-2xl border border-green-100 bg-green-50/70 p-3">
          <svg className="h-5 w-5 flex-shrink-0 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
          </svg>
          <span className="text-sm font-medium text-[#1f352e]">Protect Your Time</span>
        </div>
      </div>

      <div className="space-y-4 rounded-3xl border border-gray-100 bg-white p-4">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-xs uppercase tracking-[0.2em] text-gray-400">Reflection</p>
            <p className="text-sm font-semibold text-[#1f352e] mt-1">End of Day Q&amp;A</p>
          </div>
          <button
            type="button"
            onClick={() => setShowQna(true)}
            className="rounded-2xl bg-[#4A9782] px-4 py-2 text-xs font-semibold text-white shadow-sm shadow-[#4A9782]/30 hover:bg-[#3f7f6c]"
          >
            Open Q&amp;A
          </button>
        </div>
        <p className="text-xs text-gray-500">
          At the end of your day, capture how it went so you can improve tomorrow.
        </p>
      </div>

      {showQna && (
        <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/40 px-4 py-6" onClick={() => setShowQna(false)}>
          <div
            className="w-full max-w-sm rounded-3xl bg-white p-5 shadow-2xl animate-slideUp"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="mb-4 flex items-center justify-between">
              <div>
                <p className="text-xs uppercase tracking-[0.2em] text-gray-400">Today</p>
                <h3 className="text-lg font-semibold text-[#1f352e]">End of Day Q&amp;A</h3>
              </div>
              <button
                type="button"
                onClick={() => setShowQna(false)}
                className="flex h-9 w-9 items-center justify-center rounded-full border border-gray-200 text-gray-500 hover:text-gray-700"
                aria-label="Close"
              >
                &times;
              </button>
            </div>

            <div className="space-y-4 max-h-[60vh] overflow-y-auto pr-1">
              <div>
                <p className="text-xs font-semibold text-gray-600 mb-1">
                  1. Do you feel that you have completed your day?
                </p>
                <textarea
                  name="completed"
                  value={qnaAnswers.completed}
                  onChange={handleQnaChange}
                  rows="2"
                  className="w-full rounded-2xl border border-gray-200 bg-gray-50 px-3 py-2 text-sm focus:border-[#4A9782] focus:outline-none focus:ring-2 focus:ring-[#4A9782]/20"
                  placeholder="Write your answer..."
                />
              </div>
              <div>
                <p className="text-xs font-semibold text-gray-600 mb-1">
                  2. What mistake did you make today?
                </p>
                <textarea
                  name="mistake"
                  value={qnaAnswers.mistake}
                  onChange={handleQnaChange}
                  rows="2"
                  className="w-full rounded-2xl border border-gray-200 bg-gray-50 px-3 py-2 text-sm focus:border-[#4A9782] focus:outline-none focus:ring-2 focus:ring-[#4A9782]/20"
                  placeholder="Be honest with yourself..."
                />
              </div>
              <div>
                <p className="text-xs font-semibold text-gray-600 mb-1">
                  3. How would you summarize your day?
                </p>
                <textarea
                  name="summary"
                  value={qnaAnswers.summary}
                  onChange={handleQnaChange}
                  rows="3"
                  className="w-full rounded-2xl border border-gray-200 bg-gray-50 px-3 py-2 text-sm focus:border-[#4A9782] focus:outline-none focus:ring-2 focus:ring-[#4A9782]/20"
                  placeholder="Summary of your day..."
                />
              </div>
            </div>

            <div className="mt-4 flex gap-2">
              <button
                type="button"
                onClick={() => setShowQna(false)}
                className="flex-1 rounded-2xl border border-gray-200 bg-white py-2.5 text-sm font-semibold text-gray-600 hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleSaveQna}
                className="flex-1 rounded-2xl bg-[#4A9782] py-2.5 text-sm font-semibold text-white shadow-sm shadow-[#4A9782]/30 hover:bg-[#3f7f6c]"
              >
                Save
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default ProfileScreen;
