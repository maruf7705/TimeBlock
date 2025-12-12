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
    <div className="min-h-screen bg-[#f4f8f6] py-4 px-3 sm:py-6 sm:px-4">
      {isAppLoading && <LoadingOverlay />}
      <div className="relative z-10 max-w-[420px] w-full mx-auto border border-transparent rounded-[28px] sm:rounded-[32px] shadow-[0_32px_70px_rgba(74,151,130,0.18)] overflow-hidden bg-white/95 backdrop-blur">
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
    <div className="flex border-b border-gray-100 bg-white/90 backdrop-blur">
      {tabs.map(tab => (
        <button
          key={tab.id}
          onClick={() => setActiveView(tab.id)}
          className={`flex-1 py-4 px-3 text-[13px] sm:text-xs tracking-[0.08em] font-semibold uppercase transition-all duration-200 border-b-2 min-h-[52px] flex items-center justify-center ${
            activeView === tab.id
              ? 'text-[#4A9782] border-[#4A9782] bg-[#4A9782]/5 shadow-inner'
              : 'text-gray-400 border-transparent hover:text-gray-600 hover:bg-gray-50'
          }`}
        >
          {tab.label}
        </button>
      ))}
    </div>
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
    <div className="p-4 sm:p-5 space-y-5 sm:space-y-6">
      <form onSubmit={handleSubmit} className="space-y-4 sm:space-y-5">
        {editingTask && (
          <div className="rounded-2xl border border-[#4A9782]/30 bg-[#4A9782]/5 px-4 py-3 text-[13px] sm:text-xs font-semibold text-[#2d5b4f]">
            Editing time block
          </div>
        )}

        <div className="grid grid-cols-2 gap-3 sm:gap-4">
          <div>
            <label htmlFor="startTime" className="block text-[13px] sm:text-xs font-medium uppercase tracking-wide text-gray-500 mb-2">
              Start
            </label>
            <input
              type="time"
              id="startTime"
              name="startTime"
              value={formData.startTime}
              onChange={handleInputChange}
              className="w-full rounded-2xl border border-gray-200 bg-gray-50 px-4 py-3 text-[15px] sm:text-sm focus:border-[#4A9782] focus:outline-none focus:ring-2 focus:ring-[#4A9782]/20 min-h-[48px]"
              required
            />
          </div>
          <div>
            <label htmlFor="endTime" className="block text-[13px] sm:text-xs font-medium uppercase tracking-wide text-gray-500 mb-2">
              End
            </label>
            <input
              type="time"
              id="endTime"
              name="endTime"
              value={formData.endTime}
              onChange={handleInputChange}
              className="w-full rounded-2xl border border-gray-200 bg-gray-50 px-4 py-3 text-[15px] sm:text-sm focus:border-[#4A9782] focus:outline-none focus:ring-2 focus:ring-[#4A9782]/20 min-h-[48px]"
              required
            />
          </div>
        </div>

        <div>
          <label htmlFor="description" className="block text-[13px] sm:text-xs font-medium uppercase tracking-wide text-gray-500 mb-2">
            Description
          </label>
          <textarea
            id="description"
            name="description"
            value={formData.description}
            onChange={handleInputChange}
            rows="3"
            placeholder="What are you focusing on?"
            className="w-full rounded-2xl border border-gray-200 bg-gray-50 px-4 py-3 text-[15px] sm:text-sm focus:border-[#4A9782] focus:outline-none focus:ring-2 focus:ring-[#4A9782]/20 resize-none"
            required
          />
        </div>

        <div>
          <label htmlFor="notes" className="block text-[13px] sm:text-xs font-medium uppercase tracking-wide text-gray-500 mb-2">
            Notes
          </label>
          <textarea
            id="notes"
            name="notes"
            value={formData.notes}
            onChange={handleInputChange}
            rows="3"
            placeholder="Add extra context"
            className="w-full rounded-2xl border border-gray-200 bg-gray-50 px-4 py-3 text-[15px] sm:text-sm focus:border-[#4A9782] focus:outline-none focus:ring-2 focus:ring-[#4A9782]/20 resize-none"
          />
        </div>

        <div className="flex gap-3 sm:gap-2">
          {editingTask && (
            <button
              type="button"
              onClick={handleCancelEdit}
              className="flex-1 rounded-2xl border border-gray-200 bg-white py-3.5 sm:py-3 text-[15px] sm:text-sm font-semibold text-gray-600 transition hover:bg-gray-50 min-h-[48px]"
            >
              Cancel
            </button>
          )}
          <button
            type="submit"
            className="flex-1 rounded-2xl bg-[#4A9782] py-3.5 sm:py-3 text-[15px] sm:text-sm font-semibold text-white shadow-lg shadow-[#4A9782]/30 transition hover:bg-[#3f7f6c] min-h-[48px]"
          >
            {editingTask ? 'Update Time Block' : 'Add Time Block'}
          </button>
        </div>
      </form>

      <div>
        <button
          type="button"
          onClick={() => setShowTodayModal(true)}
          className="w-full rounded-3xl border border-gray-100 bg-white px-5 py-5 text-left shadow-sm transition hover:border-[#4A9782]/40 hover:shadow-md min-h-[72px]"
        >
          <div className="flex items-center justify-between">
            <div>
              <p className="text-[13px] sm:text-xs uppercase tracking-[0.2em] text-gray-400">Today</p>
              <p className="text-base sm:text-lg font-semibold text-[#1f352e] mt-1.5">Time Blocks</p>
            </div>
            <div className="flex items-center justify-center rounded-full bg-[#4A9782]/10 px-4 py-2 text-[14px] sm:text-xs font-semibold text-[#4A9782] min-w-[36px]">
              {todaysTasks.length}
            </div>
          </div>
        </button>
      </div>

      {showTodayModal && (
        <div className="fixed inset-0 z-40 flex items-end justify-center bg-black/40 px-3 sm:px-4 py-4 sm:py-6" onClick={() => setShowTodayModal(false)}>
          <div
            className="w-full max-w-[420px] rounded-3xl bg-white p-5 sm:p-6 shadow-2xl animate-slideUp max-h-[85vh] overflow-y-auto"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="mb-5 flex items-center justify-between">
              <div>
                <p className="text-[13px] sm:text-xs uppercase tracking-[0.2em] text-gray-400">Today</p>
                <h3 className="text-lg sm:text-xl font-semibold text-[#1f352e] mt-1">Time Blocks</h3>
              </div>
              <button
                type="button"
                onClick={() => setShowTodayModal(false)}
                className="flex h-10 w-10 items-center justify-center rounded-full border border-gray-200 text-gray-500 transition hover:text-gray-700 text-xl"
                aria-label="Close"
              >
                &times;
              </button>
            </div>

            {todaysTasks.length === 0 ? (
              <p className="py-8 text-center text-[15px] sm:text-sm text-gray-500">No time blocks added today.</p>
            ) : (
              <div className="space-y-3 sm:space-y-4">
                {todaysTasks.map(task => (
                  <div key={task.id} className="rounded-2xl border border-gray-100 bg-gray-50/70 p-4 sm:p-5 shadow-inner">
                    <div className="flex items-start justify-between gap-4">
                      <div className="flex-1 min-w-0">
                        <p className="text-[15px] sm:text-sm font-semibold text-[#1f352e] break-words">{task.description}</p>
                        <p className="mt-2 text-[13px] sm:text-xs text-gray-500">
                          {task.startTime} - {task.endTime} | {calculateDuration(task.startTime, task.endTime)}
                        </p>
                      </div>
                      <div className="flex items-center gap-3 flex-shrink-0">
                        <button
                          type="button"
                          onClick={() => handleEdit(task)}
                          className="text-[14px] sm:text-xs font-semibold text-[#4A9782] transition hover:opacity-80 px-2 py-1 min-h-[36px]"
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
                          className="text-[14px] sm:text-xs font-semibold text-red-500 transition hover:opacity-80 px-2 py-1 min-h-[36px]"
                        >
                          Delete
                        </button>
                      </div>
                    </div>
                    {task.notes && (
                      <p className="mt-3 text-[13px] sm:text-xs text-gray-600 break-words">{task.notes}</p>
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
      <div className="p-4 sm:p-5 space-y-5 sm:space-y-6">
        <div className="flex items-center justify-between rounded-3xl bg-white px-4 sm:px-5 py-4 shadow-sm">
          <button
            onClick={() => handleMonthChange(-1)}
            className="rounded-2xl border border-gray-200 px-4 sm:px-5 py-2.5 text-[14px] sm:text-sm font-semibold text-gray-600 transition hover:border-[#4A9782]/40 hover:text-[#4A9782] min-h-[44px]"
          >
            Previous
          </button>
          <div className="text-center">
            <h2 className="text-base sm:text-lg font-semibold text-[#1f352e]">{monthName}</h2>
            <button
              onClick={goToToday}
              className="mt-1.5 text-[13px] sm:text-xs font-semibold text-[#4A9782] hover:text-[#3b7c67] py-1 px-2"
            >
              Go to Today
            </button>
          </div>
          <button
            onClick={() => handleMonthChange(1)}
            className="rounded-2xl border border-gray-200 px-4 sm:px-5 py-2.5 text-[14px] sm:text-sm font-semibold text-gray-600 transition hover:border-[#4A9782]/40 hover:text-[#4A9782] min-h-[44px]"
          >
            Next
          </button>
        </div>

        <div className="overflow-hidden rounded-3xl border border-gray-100 bg-white shadow-sm">
          <div className="grid grid-cols-7 border-b border-gray-100 bg-gray-50/60">
            {weekDays.map(day => (
              <div key={day} className="p-2.5 sm:p-3 text-center text-[11px] sm:text-[12px] font-semibold uppercase tracking-[0.2em] text-gray-400">
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
                  className={`min-h-[68px] sm:min-h-[72px] border-r border-b border-gray-100 px-2 sm:px-3 py-2 text-left transition ${
                    !date ? 'bg-gray-50/60 cursor-default' : 'bg-white hover:bg-gray-50 active:bg-gray-100'
                  } ${today ? 'bg-[#4A9782]/5 border-[#4A9782]/40' : ''}`}
                >
                  {date && (
                    <>
                      <div className="mb-1.5 sm:mb-2 flex items-start justify-between gap-1.5">
                        <div
                          className={`flex h-7 w-7 sm:h-8 sm:w-8 items-center justify-center rounded-full text-[13px] sm:text-sm font-semibold ${
                            today ? 'bg-[#4A9782] text-white' : 'text-gray-700'
                          }`}
                        >
                          {date.getDate()}
                        </div>
                      </div>
                      {taskCount > 0 && (
                        <div className="flex flex-wrap gap-0.5 sm:gap-1">
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
                        <div className="mt-1 text-[11px] sm:text-xs text-gray-500">
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
  const settings = [
    { name: 'Statistics & Reports' },
    { name: 'App Settings' },
    { name: 'Export Data' },
    { name: 'Log Out' }
  ]

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
    <div className="p-4 sm:p-5 space-y-5 sm:space-y-6">
      <div className="flex flex-col items-center py-5 sm:py-6">
        <div className="mb-4 flex h-20 w-20 sm:h-24 sm:w-24 items-center justify-center rounded-full bg-[#4A9782]/10 shadow-inner">
          <svg className="h-10 w-10 sm:h-12 sm:w-12 text-[#4A9782]" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z" clipRule="evenodd" />
          </svg>
        </div>
        <h2 className="text-lg sm:text-xl font-bold text-[#1f352e]">Maruf Islam</h2>
        <p className="mt-1.5 text-[14px] sm:text-sm text-gray-500">Learning AI & ML</p>
      </div>

      <div className="rounded-3xl border border-[#4A9782]/20 bg-gradient-to-r from-[#4A9782]/10 to-[#4A9782]/5 p-5 sm:p-6">
        <div className="grid grid-cols-2 gap-6 sm:gap-8">
          <div className="text-center">
            <div className="text-3xl sm:text-4xl font-bold text-[#4A9782] mb-2">{totalTasks}</div>
            <div className="text-sm sm:text-xs uppercase tracking-[0.15em] text-gray-600 font-medium">Total Blocks</div>
          </div>
          <div className="text-center">
            <div className="text-3xl sm:text-4xl font-bold text-[#4A9782] mb-2">{totalHours.toFixed(1)}h</div>
            <div className="text-sm sm:text-xs uppercase tracking-[0.15em] text-gray-600 font-medium">Total Time</div>
          </div>
        </div>
      </div>

      <div className="space-y-3">
        <div className="flex items-center space-x-3 rounded-2xl border border-blue-100 bg-blue-50/70 p-4">
          <svg className="h-5 w-5 flex-shrink-0 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
          </svg>
          <span className="text-[15px] sm:text-sm font-medium text-[#1f352e]">AI For Management</span>
        </div>
        <div className="flex items-center space-x-3 rounded-2xl border border-yellow-100 bg-yellow-50/70 p-4">
          <svg className="h-5 w-5 flex-shrink-0 text-yellow-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <span className="text-[15px] sm:text-sm font-medium text-[#1f352e]">Manage Your Time</span>
        </div>
        <div className="flex items-center space-x-3 rounded-2xl border border-green-100 bg-green-50/70 p-4">
          <svg className="h-5 w-5 flex-shrink-0 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
          </svg>
          <span className="text-[15px] sm:text-sm font-medium text-[#1f352e]">Protect Your Time</span>
        </div>
      </div>

      <div className="space-y-2 rounded-3xl border border-gray-100 bg-white p-4">
        {settings.map((setting, index) => (
          <button
            key={index}
            className="flex w-full items-center justify-between rounded-2xl px-4 py-3.5 sm:py-3 text-left text-[15px] sm:text-sm font-semibold text-[#1f352e] transition hover:bg-gray-50 min-h-[48px]"
          >
            <span>{setting.name}</span>
            <svg
              className="h-5 w-5 sm:h-4 sm:w-4 text-gray-400"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="1.5"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <path d="M9 18l6-6-6-6" />
            </svg>
          </button>
        ))}
      </div>
    </div>
  )
}

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

export default App
