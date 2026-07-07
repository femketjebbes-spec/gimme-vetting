import React from 'react'
import { createRoot } from 'react-dom/client'
import ExcelUpload from './frontend/components/ExcelUpload'

const root = createRoot(document.getElementById('root'))
root.render(
  <React.StrictMode>
    <ExcelUpload />
  </React.StrictMode>
)
