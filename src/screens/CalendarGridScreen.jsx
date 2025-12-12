import React, { useState, useMemo, useCallback } from 'react';
import { Share } from '@capacitor/share';
import { getTodayDateString } from '../utils';
import { useTasks, useTaskActions } from '../context/TaskContext';

import { useToast } from '../context/ToastContext';

function CalendarGridScreen() {
  const tasks = useTasks();
  const { onAddTask, onUpdateTask, onDeleteTask } = useTaskActions();
  const { showToast } = useToast();
  const [currentDate, setCurrentDate] = useState(new Date())
  const [selectedDate, setSelectedDate] = useState(null)
  const [showTaskPanel, setShowTaskPanel] = useState(false)
  const [editingTask, setEditingTask] = useState(null)
  const [formData, setFormData] = useState({
    startTime: '',
    endTime: '',
    description: '',
    notes: ''
  })

  const handleInputChange = useCallback((e) => {
    const { name, value } = e.target
    setFormData(prev => ({ ...prev, [name]: value }))
  }, [])

  const formatDateToString = useCallback((date) => {
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  }, [])

  const handleDateClick = useCallback((date) => {
    if (!date) return
    const dateString = formatDateToString(date)
    setSelectedDate(dateString)
    setFormData({
      startTime: '',
      endTime: '',
      description: '',
      notes: ''
    })
    setEditingTask(null)
    setShowTaskPanel(true)
  }, [formatDateToString])

  const handleEdit = useCallback((task) => {
    if (task.date !== getTodayDateString()) return
    setEditingTask(task)
    setFormData({
      startTime: task.startTime,
      endTime: task.endTime,
      description: task.description,
      notes: task.notes || ''
    })
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

    if (selectedDate !== getTodayDateString()) {
      showToast("Only today's time blocks can be created or edited.", 'error');
      return
    }

    if (!formData.startTime || !formData.endTime || !formData.description.trim()) {
      showToast('Please fill in all required fields', 'error');
      return
    }

    if (formData.startTime >= formData.endTime) {
      showToast('End time must be after start time', 'error');
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
      onUpdateTask(editingTask.id, {
        ...payload
      })
      showToast('Time block updated!', 'success');
      handleCancelEdit()
    } else {
      onAddTask({ id: Date.now(), ...payload })
      showToast('Time block added!', 'success');
      handleCancelEdit()
    }
  }, [formData, editingTask, onAddTask, onUpdateTask, handleCancelEdit, selectedDate, showToast])

  const handleMonthChange = useCallback((direction) => {
    setCurrentDate(prev => {
      const newDate = new Date(prev)
      newDate.setMonth(prev.getMonth() + direction)
      return newDate
    })
  }, [])

  const goToToday = useCallback(() => {
    setCurrentDate(new Date())
  }, [])

  const getDaysInMonth = useCallback((date) => {
    const year = date.getFullYear()
    const month = date.getMonth()
    const firstDay = new Date(year, month, 1)
    const lastDay = new Date(year, month + 1, 0)
    const daysInMonth = lastDay.getDate()
    const startingDayOfWeek = firstDay.getDay()
    
    const days = []
    // Add empty cells for days before the first day of the month
    for (let i = 0; i < startingDayOfWeek; i++) {
      days.push(null)
    }
    // Add all days of the month
    for (let i = 1; i <= daysInMonth; i++) {
      days.push(new Date(year, month, i))
    }
    return days
  }, [])

  const getTasksForDate = useCallback((date) => {
    if (!date) return []
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const dateString = `${year}-${month}-${day}`
    return tasks.filter(task => task.date === dateString)
  }, [tasks])

  const isToday = useCallback((date) => {
    if (!date) return false
    const today = new Date()
    return date.toDateString() === today.toDateString()
  }, [])

  const selectedDateTasks = useMemo(() => {
    if (!selectedDate) return []
    return tasks
      .filter(task => task.date === selectedDate)
      .sort((a, b) => {
        if (a.startTime < b.startTime) return -1
        if (a.startTime > b.startTime) return 1
        return 0
      })
  }, [tasks, selectedDate])

  const calendarDays = useMemo(() => {
    return getDaysInMonth(currentDate)
  }, [currentDate, getDaysInMonth])

  const monthName = useMemo(() => {
    return currentDate.toLocaleDateString('en-US', { month: 'long', year: 'numeric' })
  }, [currentDate])

  const weekDays = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']

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

  const formatDateToExport = useCallback((dateString) => {
    // Convert YYYY-MM-DD to M/D/YYYY
    const [year, month, day] = dateString.split('-')
    return `${parseInt(month)}/${parseInt(day)}/${year}`
  }, [])

  const exportDayTasks = useCallback(async (date) => {
    if (!date) return

    const dateString = formatDateToString(date)
    const dayTasks = getTasksForDate(date)
      .sort((a, b) => {
        if (a.startTime < b.startTime) return -1
        if (a.startTime > b.startTime) return 1
        return 0
      })

    // Format the export content
    let content = `Date: ${formatDateToExport(dateString)}\n\n\n`

    if (dayTasks.length === 0) {
      content += 'No time blocks for this day.\n'
    } else {
      dayTasks.forEach((task, index) => {
        content += `${task.startTime} to ${task.endTime}\n`
        content += `${task.description}\n`
        content += `Notes: ${task.notes || ''}\n`
        if (index < dayTasks.length - 1) {
          content += '\n\n'
        }
      })
    }

    // Add Q&A section
    const qnaKey = `timeProtectorQna-${dateString}`
    const savedQna = localStorage.getItem(qnaKey)
    if (savedQna) {
      try {
        const qnaAnswers = JSON.parse(savedQna)
        content += '\n\n\n--- End of Day Q&A ---\n\n'
        content += `1. Do you feel that you have completed your day?\n- ${qnaAnswers.completed || 'Not answered'}\n\n`
        content += `2. What mistake did you make today?\n- ${qnaAnswers.mistake || 'Not answered'}\n\n`
        content += `3. How would you summarize your day?\n- ${qnaAnswers.summary || 'Not answered'}\n`
      } catch {
        // Ignore Q&A if parsing fails
      }
    }

    try {
      // Use Capacitor Share API for native Android sharing
      await Share.share({
        title: `Time Blocks - ${formatDateToExport(dateString)}`,
        text: content,
        dialogTitle: 'Share Time Blocks'
      })
    } catch (error) {
      // Handle errors gracefully
      console.error('Error sharing time blocks:', error)
      // Fallback: try to copy to clipboard if available
      if (navigator.clipboard) {
        try {
          await navigator.clipboard.writeText(content)
          showToast('Time blocks copied to clipboard!', 'success');
        } catch (clipboardError) {
          showToast('Unable to share time blocks. Please try again.', 'error');
        }
      } else {
        showToast('Unable to share time blocks. Please try again.', 'error');
      }
    }
  }, [formatDateToString, formatDateToExport, getTasksForDate])

  const handleExportSelectedDate = useCallback(() => {
    if (!selectedDate) return
    const [year, month, day] = selectedDate.split('-').map(Number)
    const date = new Date(year, month - 1, day)
    exportDayTasks(date)
  }, [selectedDate, exportDayTasks])

  const canEditSelectedDate = selectedDate === getTodayDateString()

  return (
    <>
      <div className="px-5 pt-5 space-y-6">
        <div className="flex items-center justify-between rounded-3xl bg-white px-4 py-3 shadow-sm">
          <button
            onClick={() => handleMonthChange(-1)}
            className="rounded-2xl border border-gray-200 px-4 py-2 text-sm font-semibold text-gray-600 transition hover:border-[#4A9782]/40 hover:text-[#4A9782]"
          >
            Previous
          </button>
          <div className="text-center">
            <h2 className="text-lg font-semibold text-[#1f352e]">{monthName}</h2>
            <button
              onClick={goToToday}
              className="mt-1 text-xs font-semibold text-[#4A9782] hover:text-[#3b7c67]"
            >
              Go to Today
            </button>
          </div>
          <button
            onClick={() => handleMonthChange(1)}
            className="rounded-2xl border border-gray-200 px-4 py-2 text-sm font-semibold text-gray-600 transition hover:border-[#4A9782]/40 hover:text-[#4A9782]"
          >
            Next
          </button>
        </div>

        <div className="overflow-hidden rounded-3xl border border-gray-100 bg-white shadow-sm">
          <div className="grid grid-cols-7 border-b border-gray-100 bg-gray-50/60">
            {weekDays.map(day => (
              <div key={day} className="p-3 text-center text-[11px] font-semibold uppercase tracking-[0.2em] text-gray-400">
                {day}
              </div>
            ))}
          </div>

          <div className="grid grid-cols-7">
            {calendarDays.map((date, index) => {
              const dateTasks = getTasksForDate(date)
              const taskCount = dateTasks.length
              const today = isToday(date)

              return (
                <button
                  key={index}
                  onClick={() => date && handleDateClick(date)}
                  disabled={!date}
                  className={`border-r border-b border-gray-100 px-3 py-2 text-left transition ${ 
                    !date ? 'bg-gray-50/60 cursor-default' : 'bg-white hover:bg-gray-50'
                  } ${today ? 'bg-[#4A9782]/5 border-[#4A9782]/40' : ''}`}
                >
                  {date && (
                    <>
                      <div className="mb-2 flex items-start justify-between gap-2">
                        <div
                          className={`flex h-7 w-7 items-center justify-center rounded-full text-sm font-semibold ${ 
                            today ? 'bg-[#4A9782] text-white' : 'text-gray-700'
                          }`}
                        >
                          {date.getDate()}
                        </div>
                      </div>
                      {taskCount > 0 && (
                        <div className="flex flex-wrap gap-1">
                          {dateTasks.slice(0, 3).map(task => (
                            <div
                              key={task.id}
                              className="h-1.5 w-full rounded-full bg-[#4A9782]/50"
                              title={task.description}
                            />
                          ))}
                          {taskCount > 3 && (
                            <div className="h-1.5 w-full rounded-full bg-[#4A9782]/25" />
                          )}
                        </div>
                      )}
                      {taskCount > 0 && (
                        <div className="mt-1 text-xs text-gray-500">
                          {taskCount} {taskCount === 1 ? 'task' : 'tasks'}
                        </div>
                      )}
                    </>
                  )}
                </button>
              )
            })}
          </div>
        </div>
      </div>

      {/* Task Panel (Slide-in) */}
      {showTaskPanel && (
        <div 
          className="fixed inset-0 z-50 flex items-end bg-black/50 backdrop-blur-sm"
          onClick={() => setShowTaskPanel(false)}
        >
          <div 
            className="max-h-[85vh] w-full animate-slideUp overflow-y-auto rounded-t-[32px] bg-white p-6 shadow-2xl"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between border-b border-gray-100 pb-4">
              <div>
                <p className="text-xs uppercase tracking-[0.2em] text-gray-400">Selected</p>
                <h2 className="text-lg font-semibold text-[#1f352e]">
                  {selectedDate ? new Date(selectedDate).toLocaleDateString('en-US', { 
                    weekday: 'long', 
                    month: 'long', 
                    day: 'numeric',
                    year: 'numeric'
                  }) : ''}
                </h2>
                <p className="mt-1 text-xs text-gray-500">
                  {selectedDateTasks.length} {selectedDateTasks.length === 1 ? 'time block' : 'time blocks'}
                </p>
              </div>
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={(e) => {
                    e.stopPropagation()
                    handleExportSelectedDate()
                  }}
                  className="flex items-center gap-1 rounded-2xl border border-[#4A9782]/40 bg-[#4A9782]/5 px-3 py-1.5 text-xs font-semibold text-[#2d5b4f] hover:bg-[#4A9782]/10"
                  disabled={!selectedDate}
                >
                  <svg
                    className="h-3.5 w-3.5"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="1.6"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  >
                    <path d="M12 5v12" />
                    <path d="M8 9l4-4 4 4" />
                    <path d="M5 19h14" />
                  </svg>
                  <span>Export</span>
                </button>
                <button
                  onClick={() => setShowTaskPanel(false)}
                  className="flex h-10 w-10 items-center justify-center rounded-full border border-gray-200 text-gray-500"
                >
                  &times;
                </button>
              </div>
            </div>

            <div className="mt-5 space-y-5">
              {canEditSelectedDate ? (
                <div className="rounded-3xl bg-gray-50 p-4">
                  {editingTask && (
                    <div className="mb-3 rounded-2xl border border-[#4A9782]/30 bg-[#4A9782]/5 px-3 py-2 text-xs font-semibold text-[#2d5b4f]">
                      Editing today's time block
                    </div>
                  )}
                <form onSubmit={handleSubmit} className="space-y-3">
                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="block text-xs font-medium text-gray-700 mb-1">Start Time</label>
                      <input
                        type="time"
                        name="startTime"
                        value={formData.startTime}
                        onChange={handleInputChange}
                        className="w-full rounded-2xl border border-gray-200 bg-white px-3 py-2 text-sm focus:border-[#4A9782] focus:ring-2 focus:ring-[#4A9782]/20"
                        required
                      />
                    </div>
                    <div>
                      <label className="block text-xs font-medium text-gray-700 mb-1">End Time</label>
                      <input
                        type="time"
                        name="endTime"
                        value={formData.endTime}
                        onChange={handleInputChange}
                        className="w-full rounded-2xl border border-gray-200 bg-white px-3 py-2 text-sm focus:border-[#4A9782] focus:ring-2 focus:ring-[#4A9782]/20"
                        required
                      />
                    </div>
                  </div>
                  <div>
                    <label className="block text-xs font-medium text-gray-700 mb-1">Description</label>
                    <input
                      type="text"
                      name="description"
                      value={formData.description}
                      onChange={handleInputChange}
                      placeholder="What did you do?"
                      className="w-full rounded-2xl border border-gray-200 bg-white px-3 py-2 text-sm focus:border-[#4A9782] focus:ring-2 focus:ring-[#4A9782]/20"
                      required
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-medium text-gray-700 mb-1">Notes (optional)</label>
                    <textarea
                      name="notes"
                      value={formData.notes}
                      onChange={handleInputChange}
                      rows="2"
                      placeholder="Additional notes..."
                      className="w-full rounded-2xl border border-gray-200 bg-white px-3 py-2 text-sm focus:border-[#4A9782] focus:ring-2 focus:ring-[#4A9782]/20"
                    />
                  </div>
                  <div className="flex gap-2">
                    {editingTask && (
                      <button
                        type="button"
                        onClick={handleCancelEdit}
                        className="flex-1 rounded-2xl border border-gray-200 bg-white py-2 text-sm font-semibold text-gray-600"
                      >
                        Cancel
                      </button>
                    )}
                    <button
                      type="submit"
                      className="flex-1 rounded-2xl bg-[#4A9782] py-2 text-sm font-semibold text-white shadow-lg shadow-[#4A9782]/25"
                    >
                      {editingTask ? 'Update' : 'Add Task'}
                    </button>
                  </div>
                </form>
                </div>
              ) : (
                <div className="rounded-3xl border border-gray-100 bg-white p-4 text-sm text-gray-500">
                  Only today's blocks can be edited. Review past or future days below.
                </div>
              )}

              <div>
                <h3 className="mb-3 text-xs font-semibold uppercase tracking-[0.2em] text-gray-400">Timeline</h3>
                {selectedDateTasks.length === 0 ? (
                  <p className="py-6 text-center text-sm text-gray-500">No time blocks recorded.</p>
                ) : (
                  <div className="space-y-2">
                    {selectedDateTasks.map(task => (
                      <div
                        key={task.id}
                        className="rounded-2xl border border-gray-100 bg-gray-50/70 p-4"
                      >
                        <div className="flex items-start justify-between gap-3">
                          <div className="flex-1">
                            <p className="text-sm font-semibold text-[#1f352e]">{task.description}</p>
                            <div className="mt-1 text-xs text-gray-500">
                              {task.startTime} - {task.endTime} | {calculateDuration(task.startTime, task.endTime)}
                            </div>
                            {task.notes && (
                              <p className="mt-2 text-xs text-gray-600">{task.notes}</p>
                            )}
                          </div>
                          {task.date === getTodayDateString() ? (
                            <div className="flex gap-2">
                              <button
                                onClick={() => handleEdit(task)}
                                className="text-xs font-semibold text-[#4A9782]"
                              >
                                Edit
                              </button>
                              <button
                                onClick={() => {
                                  if (confirm('Delete this task?')) {
                                    onDeleteTask(task.id)
                                  }
                                }}
                                className="text-xs font-semibold text-red-500"
                              >
                                Delete
                              </button>
                            </div>
                          ) : null}
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      )}
    </>
  )
}

export default CalendarGridScreen;
