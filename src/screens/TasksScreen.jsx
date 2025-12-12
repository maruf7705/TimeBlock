import React, { useState, useMemo, useCallback } from 'react';
import { getTodayDateString } from '../utils';
import { useTasks, useTaskActions } from '../context/TaskContext';
import { useToast } from '../context/ToastContext';

function TasksScreen() {
  const tasks = useTasks();
  const { onAddTask, onUpdateTask, onDeleteTask } = useTaskActions();
  const { showToast } = useToast();
  const [formData, setFormData] = useState({
    startTime: '',
    endTime: '',
    description: '',
    notes: ''
  })
  const [editingTask, setEditingTask] = useState(null)
  const [showTodayModal, setShowTodayModal] = useState(false)

  const handleInputChange = useCallback((e) => {
    const { name, value } = e.target
    setFormData(prev => ({ ...prev, [name]: value }))
  }, [])

  const handleEdit = useCallback((task) => {
    setEditingTask(task)
    setFormData({
      startTime: task.startTime,
      endTime: task.endTime,
      description: task.description,
      notes: task.notes || ''
    })
    setShowTodayModal(false)
  }, [])

  const handleCancelEdit = useCallback(() => {
    setEditingTask(null)
    setFormData({
      startTime: '',
      endTime: '',
      description: '',
      notes: ''
    })
  }, [])

  const handleSubmit = useCallback((e) => {
    e.preventDefault()

    if (!formData.startTime || !formData.endTime || !formData.description.trim()) {
      showToast('Please complete the required fields.', 'error');
      return
    }

    if (formData.startTime >= formData.endTime) {
      showToast('End time must be after start time.', 'error');
      return
    }

    const payload = {
      date: getTodayDateString(),
      startTime: formData.startTime,
      endTime: formData.endTime,
      description: formData.description.trim(),
      notes: formData.notes.trim()
    }

    if (editingTask) {
      onUpdateTask(editingTask.id, payload)
      showToast('Time block updated!', 'success');
    } else {
      onAddTask({ id: Date.now(), ...payload })
      showToast('Time block added!', 'success');
    }

    handleCancelEdit()
  }, [formData, editingTask, onAddTask, onUpdateTask, handleCancelEdit, showToast])

  const todaysTasks = useMemo(() => {
    const today = getTodayDateString()
    return tasks
      .filter(task => task.date === today)
      .sort((a, b) => a.startTime.localeCompare(b.startTime))
  }, [tasks])

  const calculateDuration = useCallback((startTime, endTime) => {
    const [startHour, startMin] = startTime.split(':').map(Number)
    const [endHour, endMin] = endTime.split(':').map(Number)
    const startMinutes = startHour * 60 + startMin
    const endMinutes = endHour * 60 + endMin
    const duration = endMinutes - startMinutes
    const hours = Math.floor(duration / 60)
    const minutes = duration % 60
    if (hours > 0 && minutes > 0) return `${hours}h ${minutes}m`
    if (hours > 0) return `${hours}h`
    return `${minutes}m`
  }, [])

  return (
    <div className="px-5 pt-5 space-y-6">
      <form onSubmit={handleSubmit} className="space-y-4">
        {editingTask && (
          <div className="rounded-2xl border border-[#4A9782]/30 bg-[#4A9782]/5 px-4 py-2 text-xs font-semibold text-[#2d5b4f]">
            Editing time block
          </div>
        )}

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label htmlFor="startTime" className="block text-xs font-medium uppercase tracking-wide text-gray-500 mb-1">
              Start
            </label>
            <input
              type="time"
              id="startTime"
              name="startTime"
              value={formData.startTime}
              onChange={handleInputChange}
              className="w-full rounded-2xl border border-gray-200 bg-gray-50 px-3 py-2 text-sm focus:border-[#4A9782] focus:outline-none focus:ring-2 focus:ring-[#4A9782]/20"
              required
            />
          </div>
          <div>
            <label htmlFor="endTime" className="block text-xs font-medium uppercase tracking-wide text-gray-500 mb-1">
              End
            </label>
            <input
              type="time"
              id="endTime"
              name="endTime"
              value={formData.endTime}
              onChange={handleInputChange}
              className="w-full rounded-2xl border border-gray-200 bg-gray-50 px-3 py-2 text-sm focus:border-[#4A9782] focus:outline-none focus:ring-2 focus:ring-[#4A9782]/20"
              required
            />
          </div>
        </div>

        <div>
          <label htmlFor="description" className="block text-xs font-medium uppercase tracking-wide text-gray-500 mb-1">
            Description
          </label>
          <textarea
            id="description"
            name="description"
            value={formData.description}
            onChange={handleInputChange}
            rows="3"
            placeholder="What are you focusing on?"
            className="w-full rounded-2xl border border-gray-200 bg-gray-50 px-3 py-2 text-sm focus:border-[#4A9782] focus:outline-none focus:ring-2 focus:ring-[#4A9782]/20"
            required
          />
        </div>

        <div>
          <label htmlFor="notes" className="block text-xs font-medium uppercase tracking-wide text-gray-500 mb-1">
            Notes
          </label>
          <textarea
            id="notes"
            name="notes"
            value={formData.notes}
            onChange={handleInputChange}
            rows="3"
            placeholder="Add extra context"
            className="w-full rounded-2xl border border-gray-200 bg-gray-50 px-3 py-2 text-sm focus:border-[#4A9782] focus:outline-none focus:ring-2 focus:ring-[#4A9782]/20"
          />
        </div>

        <div className="flex gap-2">
          {editingTask && (
            <button
              type="button"
              onClick={handleCancelEdit}
              className="flex-1 rounded-2xl border border-gray-200 bg-white py-3 text-sm font-semibold text-gray-600 transition hover:bg-gray-50"
            >
              Cancel
            </button>
          )}
          <button
            type="submit"
            className="flex-1 rounded-2xl bg-[#4A9782] py-3 text-sm font-semibold text-white shadow-lg shadow-[#4A9782]/30 transition hover:bg-[#3f7f6c]"
          >
            {editingTask ? 'Update Time Block' : 'Add Time Block'}
          </button>
        </div>
      </form>

      <div>
        <button
          type="button"
          onClick={() => setShowTodayModal(true)}
          className="w-full rounded-3xl border border-gray-100 bg-white px-4 py-4 text-left shadow-sm transition hover:border-[#4A9782]/40 hover:shadow-md"
        >
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs uppercase tracking-[0.2em] text-gray-400">Today</p>
              <p className="text-base font-semibold text-[#1f352e] mt-1">Time Blocks</p>
            </div>
            <div className="flex items-center justify-center rounded-full bg-[#4A9782]/10 px-3 py-1 text-xs font-semibold text-[#4A9782]">
              {todaysTasks.length}
            </div>
          </div>
        </button>
      </div>

      {showTodayModal && (
        <div className="fixed inset-0 z-40 flex items-end justify-center modal-backdrop" onClick={() => setShowTodayModal(false)}>
          <div
            className="w-full bg-white p-5 animate-slideUp modal-content"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="mb-4 flex items-center justify-between">
              <div>
                <p className="text-xs uppercase tracking-[0.2em] text-gray-400">Today</p>
                <h3 className="text-lg font-semibold text-[#1f352e]">Time Blocks</h3>
              </div>
              <button
                type="button"
                onClick={() => setShowTodayModal(false)}
                className="flex h-9 w-9 items-center justify-center rounded-full border border-gray-200 text-gray-500 transition hover:text-gray-700"
                aria-label="Close"
              >
                &times;
              </button>
            </div>

            {todaysTasks.length === 0 ? (
              <p className="py-6 text-center text-sm text-gray-500">No time blocks added today.</p>
            ) : (
              <div className="space-y-3">
                {todaysTasks.map(task => (
                  <div key={task.id} className="rounded-2xl border border-gray-100 bg-gray-50/70 p-4 shadow-inner">
                    <div className="flex items-start justify-between gap-3">
                      <div className="flex-1">
                        <p className="text-sm font-semibold text-[#1f352e]">{task.description}</p>
                        <p className="mt-1 text-xs text-gray-500">
                          {task.startTime} - {task.endTime} | {calculateDuration(task.startTime, task.endTime)}
                        </p>
                      </div>
                      <div className="flex items-center gap-2">
                        <button
                          type="button"
                          onClick={() => handleEdit(task)}
                          className="text-xs font-semibold text-[#4A9782] transition hover:opacity-80"
                        >
                          Edit
                        </button>
                        <button
                          type="button"
                          onClick={() => {
                            if (confirm('Delete this time block?')) {
                              onDeleteTask(task.id)
                            }
                          }}
                          className="text-xs font-semibold text-red-500 transition hover:opacity-80"
                        >
                          Delete
                        </button>
                      </div>
                    </div>
                    {task.notes && (
                      <p className="mt-2 text-xs text-gray-600">{task.notes}</p>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  )
}

export default TasksScreen;
