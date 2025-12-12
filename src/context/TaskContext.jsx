import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';

const TaskContext = createContext(null);
const TaskActionsContext = createContext(null);

export function useTasks() {
  return useContext(TaskContext);
}

export function useTaskActions() {
  return useContext(TaskActionsContext);
}

export function TaskProvider({ children }) {
  const [tasks, setTasks] = useState(() => {
    // Load from localStorage on mount
    const saved = localStorage.getItem('timeProtectorTasks');
    try {
      return saved ? JSON.parse(saved) : [];
    } catch (error) {
      console.error("Error parsing tasks from localStorage", error);
      return [];
    }
  });

  // Save to localStorage whenever tasks change
  useEffect(() => {
    localStorage.setItem('timeProtectorTasks', JSON.stringify(tasks));
  }, [tasks]);

  const onAddTask = useCallback((newTask) => {
    setTasks(prevTasks => [...prevTasks, newTask]);
  }, []);

  const onUpdateTask = useCallback((taskId, updatedTask) => {
    setTasks(prevTasks =>
      prevTasks.map(task => task.id === taskId ? { ...task, ...updatedTask } : task)
    );
  }, []);

  const onDeleteTask = useCallback((taskId) => {
    setTasks(prevTasks => prevTasks.filter(task => task.id !== taskId));
  }, []);

  return (
    <TaskContext.Provider value={tasks}>
      <TaskActionsContext.Provider value={{ onAddTask, onUpdateTask, onDeleteTask }}>
        {children}
      </TaskActionsContext.Provider>
    </TaskContext.Provider>
  );
}