import React from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import ExcelUpload from './client-service/components/ExcelUpload'
import AnalystDashboard from './business-service/components/AnalystDashboard'

import './business-service/css/analyst-dashboard.css'

const root = createRoot(document.getElementById('root'))
root.render(
  <React.StrictMode>
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<ExcelUpload />} />
        <Route path="/analyst" element={<AnalystDashboard />} />
      </Routes>
    </BrowserRouter>
  </React.StrictMode>
)
