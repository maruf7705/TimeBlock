import { useState, useMemo, useCallback, useEffect } from 'react'
import { Share } from '@capacitor/share'

const getTodayDateString = () => {
  const today = new Date()
  const year = today.getFullYear()
  const month = String(today.getMonth() + 1).padStart(2, '0')
  const day = String(today.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function App() {
  const [activeView, setActiveView] = useState('tasks')
  const [tasks, setTasks] = useState(() => {
    // Load from localStorage on mount
    const saved = localStorage.getItem('timeProtectorTasks')
    return saved ? JSON.parse(saved) : []
  })

  // Save to localStorage whenever tasks change
  useEffect(() => {
    localStorage.setItem('timeProtectorTasks', JSON.stringify(tasks))
  }, [tasks])

  const [isAppLoading, setIsAppLoading] = useState(true)

  useEffect(() => {
    const timer = setTimeout(() => setIsAppLoading(false), 900)
    return () => clearTimeout(timer)
  }, [])

  const handleAddTask = useCallback((newTask) => {
    setTasks(prevTasks => [...prevTasks, newTask])
  }, [])

  const handleUpdateTask = useCallback((taskId, updatedTask) => {
    setTasks(prevTasks =>
      prevTasks.map(task => task.id === taskId ? { ...task, ...updatedTask } : task)
    )
  }, [])

  const handleDeleteTask = useCallback((taskId) => {
    setTasks(prevTasks => prevTasks.filter(task => task.id !== taskId))
  }, [])

  return (
    <div className="app-root">
      {isAppLoading && <LoadingOverlay />}
      <div className="app-container">
        <Header activeView={activeView} setActiveView={setActiveView} />
        <ActiveScreen
          activeView={activeView}
          tasks={tasks}
          setTasks={setTasks}
          onAddTask={handleAddTask}
          onUpdateTask={handleUpdateTask}
          onDeleteTask={handleDeleteTask}
        />
      </div>
    </div>
  )
}

function Header({ activeView, setActiveView }) {
  const tabs = [
    { id: 'tasks', label: 'Tasks' },
    { id: 'calendar', label: 'Calendar' },
    { id: 'profile', label: 'Profile' }
  ]

  return (
    <header className="app-header">
      <nav className="tab-navigation">
        {tabs.map(tab => (
          <button
            key={tab.id}
            onClick={() => setActiveView(tab.id)}
            className={`tab-button ${activeView === tab.id ? 'active' : ''}`}
          >
            {tab.label}
          </button>
        ))}
      </nav>
    </header>
  )
}

function ActiveScreen({ activeView, tasks, setTasks, onAddTask, onUpdateTask, onDeleteTask }) {
  switch (activeView) {
    case 'tasks':
      return <TasksScreen tasks={tasks} onAddTask={onAddTask} onUpdateTask={onUpdateTask} onDeleteTask={onDeleteTask} />
    case 'calendar':
      return <CalendarGridScreen tasks={tasks} onAddTask={onAddTask} onUpdateTask={onUpdateTask} onDeleteTask={onDeleteTask} />
    case 'profile':
      return <ProfileScreen tasks={tasks} />
    default:
      return <TasksScreen tasks={tasks} onAddTask={onAddTask} onUpdateTask={onUpdateTask} onDeleteTask={onDeleteTask} />
  }
}

function TasksScreen({ tasks, onAddTask, onUpdateTask, onDeleteTask }) {
  const [formData, setFormData] = useState({
    startTime: '',
    endTime: '',
    description: '',
    notes: ''
  })
  const [editingTask, setEditingTask] = useState(null)
  const [showTodayModal, setShowTodayModal] = useState(false)

  // Auto-suggest start and end times
  useEffect(() => {
    // Only apply auto-suggest when not editing and form is empty
    if (!editingTask && !formData.startTime && !formData.endTime) {
      const now = new Date()
      const currentTime = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`

      // Get today's tasks sorted by time
      const today = getTodayDateString()
      const todayTasks = tasks
        .filter(task => task.date === today)
        .sort((a, b) => a.startTime.localeCompare(b.startTime))

      // Determine start time
      let suggestedStartTime = '00:00'
      if (todayTasks.length > 0) {
        // Use the end time of the last time block
        const lastTask = todayTasks[todayTasks.length - 1]
        suggestedStartTime = lastTask.endTime
      }

      setFormData(prev => ({
        ...prev,
        startTime: suggestedStartTime,
        endTime: currentTime
      }))
    }
  }, [editingTask, tasks, formData.startTime, formData.endTime])

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
      alert('Please complete the required fields.')
      return
    }

    if (formData.startTime >= formData.endTime) {
      alert('End time must be after start time.')
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
    } else {
      onAddTask({ id: Date.now(), ...payload })
    }

    handleCancelEdit()
  }, [formData, editingTask, onAddTask, onUpdateTask, handleCancelEdit])

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
    <div className="screen-container tasks-screen">
      <form onSubmit={handleSubmit} className="form-group">
        {editingTask && (
          <div className="alert alert-info">
            <span className="alert-text">Editing time block</span>
          </div>
        )}

        <div className="form-grid-2">
          <div className="input-wrapper">
            <label htmlFor="startTime" className="input-label">
              Start Time
            </label>
            <input
              type="time"
              id="startTime"
              name="startTime"
              value={formData.startTime}
              onChange={handleInputChange}
              className="input-field time-input"
              required
            />
          </div>
          <div className="input-wrapper">
            <label htmlFor="endTime" className="input-label">
              End Time
            </label>
            <input
              type="time"
              id="endTime"
              name="endTime"
              value={formData.endTime}
              onChange={handleInputChange}
              className="input-field time-input"
              required
            />
          </div>
        </div>

        <div className="input-wrapper">
          <label htmlFor="description" className="input-label">
            Description
          </label>
          <textarea
            id="description"
            name="description"
            value={formData.description}
            onChange={handleInputChange}
            rows="3"
            placeholder="What are you focusing on?"
            className="textarea-field"
            required
          />
        </div>

        <div className="input-wrapper">
          <label htmlFor="notes" className="input-label">
            Notes (Optional)
          </label>
          <textarea
            id="notes"
            name="notes"
            value={formData.notes}
            onChange={handleInputChange}
            rows="3"
            placeholder="Add extra context"
            className="textarea-field notes-field"
          />
        </div>

        <div className="button-group">
          {editingTask && (
            <button
              type="button"
              onClick={handleCancelEdit}
              className="btn btn-secondary"
            >
              Cancel
            </button>
          )}
          <button
            type="submit"
            className="btn btn-primary"
          >
            {editingTask ? 'Update Time Block' : 'Add Time Block'}
          </button>
        </div>
      </form>

      <button
        type="button"
        onClick={() => setShowTodayModal(true)}
        className="summary-card tasks-summary"
      >
        <div className="summary-content">
          <div className="summary-text">
            <span className="summary-label">Today</span>
            <h3 className="summary-title">Time Blocks</h3>
          </div>
          <div className="summary-badge">{todaysTasks.length}</div>
        </div>
      </button>

      {showTodayModal && (
        <div className="modal-backdrop" onClick={() => setShowTodayModal(false)}>
          <div
            className="modal-sheet"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="sheet-header">
              <div className="sheet-title-group">
                <span className="sheet-label">Today</span>
                <h3 className="sheet-title">Time Blocks</h3>
              </div>
              <button
                type="button"
                onClick={() => setShowTodayModal(false)}
                className="btn-close"
                aria-label="Close"
              >
                &times;
              </button>
            </div>

            <div className="sheet-content">
              {todaysTasks.length === 0 ? (
                <p className="empty-state">No time blocks added today.</p>
              ) : (
                <div className="task-list">
                  {todaysTasks.map(task => (
                    <div key={task.id} className="task-item">
                      <div className="task-header">
                        <div className="task-info">
                          <p className="task-title">{task.description}</p>
                          <p className="task-time">
                            {task.startTime} - {task.endTime} | {calculateDuration(task.startTime, task.endTime)}
                          </p>
                        </div>
                        <div className="task-actions">
                          <button
                            type="button"
                            onClick={() => handleEdit(task)}
                            className="btn-task-action edit"
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
                            className="btn-task-action delete"
                          >
                            Delete
                          </button>
                        </div>
                      </div>
                      {task.notes && (
                        <p className="task-notes">{task.notes}</p>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

function CalendarGridScreen({ tasks, onAddTask, onUpdateTask, onDeleteTask }) {
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
      alert("Only today's time blocks can be created or edited.")
      return
    }

    if (!formData.startTime || !formData.endTime || !formData.description.trim()) {
      alert('Please fill in all required fields')
      return
    }

    if (formData.startTime >= formData.endTime) {
      alert('End time must be after start time')
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
      handleCancelEdit()
    } else {
      onAddTask({ id: Date.now(), ...payload })
      handleCancelEdit()
    }
  }, [formData, editingTask, onAddTask, onUpdateTask, handleCancelEdit, selectedDate])

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

    try {
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
          const dateString = formatDateToString(date)
          const dayTasks = getTasksForDate(date)
            .sort((a, b) => {
              if (a.startTime < b.startTime) return -1
              if (a.startTime > b.startTime) return 1
              return 0
            })

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

          await navigator.clipboard.writeText(content)
          alert('Time blocks copied to clipboard!')
        } catch (clipboardError) {
          alert('Unable to share time blocks. Please try again.')
        }
      } else {
        alert('Unable to share time blocks. Please try again.')
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
      <div className="screen-container">
        <div className="calendar-header">
          <button
            onClick={() => handleMonthChange(-1)}
            className="btn-nav btn-nav-prev"
          >
            Previous
          </button>
          <div className="month-display">
            <h2 className="month-title">{monthName}</h2>
            <button
              onClick={goToToday}
              className="btn-today"
            >
              Go to Today
            </button>
          </div>
          <button
            onClick={() => handleMonthChange(1)}
            className="btn-nav btn-nav-next"
          >
            Next
          </button>
        </div>

        <div className="calendar-grid-container">
          <div className="weekday-headers">
            {weekDays.map(day => (
              <div key={day} className="weekday-header">
                {day}
              </div>
            ))}
          </div>

          <div className="calendar-grid">
            {calendarDays.map((date, index) => {
              const dateTasks = getTasksForDate(date)
              const taskCount = dateTasks.length
              const today = isToday(date)

              return (
                <button
                  key={index}
                  onClick={() => date && handleDateClick(date)}
                  disabled={!date}
                  className={`date-cell ${!date ? 'empty' : ''} ${today ? 'today' : ''}`}
                >
                  {date && (
                    <>
                      <div className="date-number">
                        {date.getDate()}
                      </div>
                      {taskCount > 0 && (
                        <div className="task-indicators">
                          {dateTasks.slice(0, 3).map(task => (
                            <div
                              key={task.id}
                              className="task-dot"
                              title={task.description}
                            />
                          ))}
                          {taskCount > 3 && (
                            <div className="task-dot" />
                          )}
                        </div>
                      )}
                      {taskCount > 0 && (
                        <div className="task-count">
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
            className="max-h-[85vh] w-full animate-slideUp overflow-y-auto rounded-t-[28px] sm:rounded-t-[32px] bg-white p-5 sm:p-6 shadow-2xl"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between border-b border-gray-100 pb-4 sm:pb-5">
              <div className="flex-1 min-w-0 pr-3">
                <p className="text-[13px] sm:text-xs uppercase tracking-[0.2em] text-gray-400">Selected</p>
                <h2 className="text-base sm:text-lg font-semibold text-[#1f352e] mt-1 break-words">
                  {selectedDate ? new Date(selectedDate).toLocaleDateString('en-US', {
                    weekday: 'long',
                    month: 'long',
                    day: 'numeric',
                    year: 'numeric'
                  }) : ''}
                </h2>
                <p className="mt-1.5 text-[13px] sm:text-xs text-gray-500">
                  {selectedDateTasks.length} {selectedDateTasks.length === 1 ? 'time block' : 'time blocks'}
                </p>
              </div>
              <div className="flex items-center gap-2 sm:gap-3 flex-shrink-0">
                <button
                  type="button"
                  onClick={(e) => {
                    e.stopPropagation()
                    handleExportSelectedDate()
                  }}
                  className="flex items-center gap-1.5 rounded-2xl border border-[#4A9782]/40 bg-[#4A9782]/5 px-3 sm:px-4 py-2 text-[13px] sm:text-xs font-semibold text-[#2d5b4f] hover:bg-[#4A9782]/10 min-h-[40px]"
                  disabled={!selectedDate}
                >
                  <svg
                    className="h-4 w-4 sm:h-3.5 sm:w-3.5"
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
                  className="flex h-10 w-10 items-center justify-center rounded-full border border-gray-200 text-gray-500 hover:text-gray-700 text-xl"
                >
                  &times;
                </button>
              </div>
            </div>

            <div className="mt-5 sm:mt-6 space-y-5">
              {canEditSelectedDate ? (
                <div className="rounded-3xl bg-gray-50 p-4 sm:p-5">
                  {editingTask && (
                    <div className="mb-4 rounded-2xl border border-[#4A9782]/30 bg-[#4A9782]/5 px-4 py-3 text-[13px] sm:text-xs font-semibold text-[#2d5b4f]">
                      Editing today's time block
                    </div>
                  )}
                  <form onSubmit={handleSubmit} className="space-y-4">
                    <div className="grid grid-cols-2 gap-3 sm:gap-4">
                      <div>
                        <label className="block text-[13px] sm:text-xs font-medium text-gray-700 mb-2">Start Time</label>
                        <input
                          type="time"
                          name="startTime"
                          value={formData.startTime}
                          onChange={handleInputChange}
                          className="w-full rounded-2xl border border-gray-200 bg-white px-4 py-3 text-[15px] sm:text-sm focus:border-[#4A9782] focus:ring-2 focus:ring-[#4A9782]/20 min-h-[48px]"
                          required
                        />
                      </div>
                      <div>
                        <label className="block text-[13px] sm:text-xs font-medium text-gray-700 mb-2">End Time</label>
                        <input
                          type="time"
                          name="endTime"
                          value={formData.endTime}
                          onChange={handleInputChange}
                          className="w-full rounded-2xl border border-gray-200 bg-white px-4 py-3 text-[15px] sm:text-sm focus:border-[#4A9782] focus:ring-2 focus:ring-[#4A9782]/20 min-h-[48px]"
                          required
                        />
                      </div>
                    </div>
                    <div>
                      <label className="block text-[13px] sm:text-xs font-medium text-gray-700 mb-2">Description</label>
                      <input
                        type="text"
                        name="description"
                        value={formData.description}
                        onChange={handleInputChange}
                        placeholder="What did you do?"
                        className="w-full rounded-2xl border border-gray-200 bg-white px-4 py-3 text-[15px] sm:text-sm focus:border-[#4A9782] focus:ring-2 focus:ring-[#4A9782]/20 min-h-[48px]"
                        required
                      />
                    </div>
                    <div>
                      <label className="block text-[13px] sm:text-xs font-medium text-gray-700 mb-2">Notes (optional)</label>
                      <textarea
                        name="notes"
                        value={formData.notes}
                        onChange={handleInputChange}
                        rows="2"
                        placeholder="Additional notes..."
                        className="w-full rounded-2xl border border-gray-200 bg-white px-4 py-3 text-[15px] sm:text-sm focus:border-[#4A9782] focus:ring-2 focus:ring-[#4A9782]/20 resize-none"
                      />
                    </div>
                    <div className="flex gap-3 sm:gap-2">
                      {editingTask && (
                        <button
                          type="button"
                          onClick={handleCancelEdit}
                          className="flex-1 rounded-2xl border border-gray-200 bg-white py-3 sm:py-2 text-[15px] sm:text-sm font-semibold text-gray-600 min-h-[48px]"
                        >
                          Cancel
                        </button>
                      )}
                      <button
                        type="submit"
                        className="flex-1 rounded-2xl bg-[#4A9782] py-3 sm:py-2 text-[15px] sm:text-sm font-semibold text-white shadow-lg shadow-[#4A9782]/25 min-h-[48px]"
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

function ProfileScreen({ tasks }) {
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

  return (
    <div className="screen-container">
      <div className="profile-header">
        <div className="profile-avatar">
          <svg className="h-12 w-12 text-[#4A9782]" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z" clipRule="evenodd" />
          </svg>
        </div>
        <h2 className="profile-name">Maruf Islam</h2>
        <p className="profile-bio">Learning AI & ML</p>
      </div>

      <div className="stats-card">
        <div className="stat-item">
          <div className="stat-value">{totalTasks}</div>
          <div className="stat-label">Total Blocks</div>
        </div>
        <div className="stat-item">
          <div className="stat-value">{totalHours.toFixed(1)}h</div>
          <div className="stat-label">Total Time</div>
        </div>
      </div>
    </div>
  )
}

function LoadingOverlay() {
  return (
    <div className="loading-overlay">
      <div className="loading-card">
        <div className="loading-spinner" />
        <p className="loading-text">Loading</p>
      </div>
    </div>
  )
}

export default App
