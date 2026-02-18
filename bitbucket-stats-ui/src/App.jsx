import { useState, useEffect } from 'react'
import Dashboard from './components/Dashboard'

const STORAGE_KEY = 'bitbucket-stats-credentials'

function App() {
  const [credentials, setCredentials] = useState(() => {
    const saved = localStorage.getItem(STORAGE_KEY)
    return saved ? JSON.parse(saved) : null
  })

  useEffect(() => {
    if (credentials) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(credentials))
    } else {
      localStorage.removeItem(STORAGE_KEY)
    }
  }, [credentials])

  return (
    <div className="min-h-screen">
      <Dashboard credentials={credentials} setCredentials={setCredentials} />
    </div>
  )
}

export default App
