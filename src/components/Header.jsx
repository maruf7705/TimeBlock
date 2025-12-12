import React from 'react';
import { TasksIcon } from './icons/TasksIcon';
import { CalendarIcon } from './icons/CalendarIcon';
import { ProfileIcon } from './icons/ProfileIcon';

function Header({ activeView, setActiveView }) {
  const tabs = [
    { id: 'tasks', label: 'Tasks', icon: TasksIcon },
    { id: 'calendar', label: 'Calendar', icon: CalendarIcon },
    { id: 'profile', label: 'Profile', icon: ProfileIcon }
  ]

  return (
    <div className="flex border-b border-gray-100 bg-white/90 backdrop-blur">
      {tabs.map(tab => {
        const Icon = tab.icon;
        const isActive = activeView === tab.id;
        return (
          <button
            key={tab.id}
            onClick={() => setActiveView(tab.id)}
            className={`flex-1 flex flex-col items-center justify-center pt-3 pb-2 text-xs transition-colors duration-200 ${
              isActive
                ? 'text-[#4A9782]'
                : 'text-gray-400 hover:text-gray-600'
            }`}
          >
            <Icon className="h-6 w-6" />
            <span className="mt-1 tracking-wider">{tab.label}</span>
            <div className={`mt-1 h-1 w-8 rounded-full ${isActive ? 'bg-[#4A9782]' : 'bg-transparent'}`} />
          </button>
        )
      })}
    </div>
  )
}

export default Header;
