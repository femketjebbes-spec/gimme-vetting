/**
 * Client-side application entry point.
 * This is the main entry file for the client-facing Vite application.
 */

import React from 'react'
import ReactDOM from 'react-dom/client'

const App = () => {
  return (
    <div>
      <h1>Client Portal</h1>
    </div>
  )
}

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
)
