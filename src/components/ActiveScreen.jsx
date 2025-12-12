import React from 'react';
import TasksScreen from '../screens/TasksScreen';
import CalendarGridScreen from '../screens/CalendarGridScreen';
import ProfileScreen from '../screens/ProfileScreen';

function ActiveScreen({ activeView }) {
  switch (activeView) {
    case 'tasks':
      return <TasksScreen />;
    case 'calendar':
      return <CalendarGridScreen />;
    case 'profile':
      return <ProfileScreen />;
    default:
      return <TasksScreen />;
  }
}

export default ActiveScreen;