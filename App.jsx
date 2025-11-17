
import React, { useState, useMemo, useCallback } from 'react';

const App = () => {
  const [activeView, setActiveView] = useState('tasks');
  const [tasks, setTasks] = useState([
    { id: 1, date: '2025-11-17', startTime: '10:00', endTime: '11:00', description: 'Team Meeting', notes: 'Discuss project milestones.' },
    { id: 2, date: '2025-11-17', startTime: '14:00', endTime: '15:30', description: 'Code Review', notes: 'Review PR #123.' },
  ]);

  const ActiveScreen = () => {
    switch (activeView) {
      case 'tasks':
        return <TasksScreen tasks={tasks} setTasks={setTasks} />;
      case 'calendar':
        return <CalendarScreen tasks={tasks} />;
      case 'profile':
        return <ProfileScreen />;
      default:
        return <TasksScreen tasks={tasks} setTasks={setTasks} />;
    }
  };

  return (
    <div className="font-sans bg-gray-50 flex items-center justify-center min-h-screen">
      <div className="w-full max-w-sm mx-auto border rounded-lg shadow-lg overflow-hidden bg-white">
        <Header activeView={activeView} setActiveView={setActiveView} />
        <div className="p-4">
          <ActiveScreen />
        </div>
      </div>
    </div>
  );
};

const Header = ({ activeView, setActiveView }) => {
  const tabs = ['tasks', 'calendar', 'profile'];
  return (
    <nav className="flex justify-around bg-gray-100 p-2">
      {tabs.map(tab => (
        <button
          key={tab}
          onClick={() => setActiveView(tab)}
          className={`capitalize text-lg font-semibold p-2 rounded-md transition-colors ${
            activeView === tab ? 'bg-purple-200 text-purple-800' : 'text-gray-600 hover:bg-gray-200'
          }`}
        >
          {tab}
        </button>
      ))}
    </nav>
  );
};

const TasksScreen = ({ tasks, setTasks }) => {
  const [date, setDate] = useState(new Date().toISOString().split('T')[0]);
  const [startTime, setStartTime] = useState('');
  const [endTime, setEndTime] = useState('');
  const [description, setDescription] = useState('');
  const [notes, setNotes] = useState('');

  const handleAddTask = useCallback(() => {
    if (!date || !startTime || !endTime || !description) {
      alert('Please fill all required fields.');
      return;
    }
    const newTask = {
      id: tasks.length + 1,
      date,
      startTime,
      endTime,
      description,
      notes,
    };
    setTasks([...tasks, newTask]);
    setDate(new Date().toISOString().split('T')[0]);
    setStartTime('');
    setEndTime('');
    setDescription('');
    setNotes('');
  }, [date, startTime, endTime, description, notes, tasks, setTasks]);

  const todaysTasks = useMemo(() => tasks.filter(task => task.date === date), [tasks, date]);

  return (
    <div>
      <div className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-700">Date</label>
          <input type="date" value={date} onChange={e => setDate(e.target.value)} className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-300 focus:ring focus:ring-indigo-200 focus:ring-opacity-50" />
        </div>
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700">Start Time</label>
            <input type="time" value={startTime} onChange={e => setStartTime(e.target.value)} className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-300 focus:ring focus:ring-indigo-200 focus:ring-opacity-50" />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700">End Time</label>
            <input type="time" value={endTime} onChange={e => setEndTime(e.target.value)} className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-300 focus:ring focus:ring-indigo-200 focus:ring-opacity-50" />
          </div>
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700">Task Description</label>
          <textarea value={description} onChange={e => setDescription(e.target.value)} className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-300 focus:ring focus:ring-indigo-200 focus:ring-opacity-50" rows="3"></textarea>
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700">Task Notes</label>
          <textarea value={notes} onChange={e => setNotes(e.target.value)} className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-300 focus:ring focus:ring-indigo-200 focus:ring-opacity-50" rows="2"></textarea>
        </div>
        <button onClick={handleAddTask} className="w-full bg-purple-200 text-purple-800 font-semibold py-2 px-4 rounded-md hover:bg-purple-300 transition-colors">
          Add time Block
        </button>
      </div>
      <div className="mt-6">
        <h2 className="text-lg font-semibold text-gray-800">Today's Tasks</h2>
        <div className="mt-4 space-y-4">
          {todaysTasks.map(task => (
            <div key={task.id} className="bg-gray-100 p-4 rounded-md">
              <p className="font-semibold">{task.description}</p>
              <p className="text-sm text-gray-600">{task.startTime} - {task.endTime}</p>
              <p className="text-sm text-gray-500 mt-1">{task.notes}</p>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

const CalendarScreen = ({ tasks }) => {
    const [selectedDate, setSelectedDate] = useState(new Date());
  
    const handleDateChange = (days) => {
      setSelectedDate(prev => {
        const newDate = new Date(prev);
        newDate.setDate(newDate.getDate() + days);
        return newDate;
      });
    };
  
    const tasksForSelectedDate = useMemo(() => {
      const dateString = selectedDate.toISOString().split('T')[0];
      return tasks.filter(task => task.date === dateString);
    }, [tasks, selectedDate]);
  
    const hours = Array.from({ length: 11 }, (_, i) => 8 + i); // 8 AM to 6 PM
  
    const getTaskPosition = (task) => {
        const [startHour, startMinute] = task.startTime.split(':').map(Number);
        const [endHour, endMinute] = task.endTime.split(':').map(Number);
      
        const top = ((startHour - 8) * 60 + startMinute) / (11 * 60) * 100;
        const height = ((endHour * 60 + endMinute) - (startHour * 60 + startMinute)) / (11 * 60) * 100;
      
        return { top: `${top}%`, height: `${height}%` };
      };
  
    return (
      <div>
        <div className="flex items-center justify-between mb-4">
          <button onClick={() => handleDateChange(-1)} className="p-2 rounded-md hover:bg-gray-200">
            &lt;
          </button>
          <h2 className="text-lg font-semibold">{selectedDate.toDateString()}</h2>
          <button onClick={() => handleDateChange(1)} className="p-2 rounded-md hover:bg-gray-200">
            &gt;
          </button>
        </div>
        <div className="relative h-[500px] bg-gray-50 rounded-lg overflow-hidden">
          {hours.map(hour => (
            <div key={hour} className="flex items-center border-b border-gray-200" style={{ height: `${100 / 11}%` }}>
              <span className="text-xs text-gray-500 -translate-y-1/2">{`${String(hour).padStart(2, '0')}:00`}</span>
            </div>
          ))}
           {tasksForSelectedDate.map(task => {
            const { top, height } = getTaskPosition(task);
            return (
              <div
                key={task.id}
                className="absolute left-12 right-0 bg-green-200 p-2 rounded-lg overflow-hidden"
                style={{ top, height }}
              >
                <p className="text-sm font-semibold text-green-800">{task.description}</p>
              </div>
            );
          })}
        </div>
      </div>
    );
  };
  

const ProfileScreen = () => {
    return (
      <div className="space-y-6">
        <div className="flex items-center space-x-4">
          <div className="w-16 h-16 bg-gray-300 rounded-full"></div>
          <div>
            <h2 className="text-xl font-bold">Maruf Islam</h2>
            <p className="text-gray-600">Learning AI & ML</p>
          </div>
        </div>
        <div className="space-y-4">
            <h3 className="font-semibold">App Philosophy</h3>
          <div className="flex items-center space-x-3">
            <svg className="w-6 h-6 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path></svg>
            <span>Ai For Management</span>
          </div>
          <div className="flex items-center space-x-3">
            <svg className="w-6 h-6 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
            <span>Manage Your Time</span>
          </div>
          <div className="flex items-center space-x-3">
            <svg className="w-6 h-6 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"></path></svg>
            <span>Protect Your Time</span>
          </div>
        </div>
        <div className="space-y-2">
            <h3 className="font-semibold">Settings</h3>
          <button className="w-full text-left p-2 rounded-md hover:bg-gray-100">Statistics & Reports</button>
          <button className="w-full text-left p-2 rounded-md hover:bg-gray-100">App Settings</button>
          <button className="w-full text-left p-2 rounded-md hover:bg-gray-100">Export Data</button>
          <button className="w-full text-left p-2 rounded-md text-red-600 hover:bg-red-100">Log Out</button>
        </div>
      </div>
    );
  };

export default App;
