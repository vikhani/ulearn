import { Route, Routes } from 'react-router-dom'

function App() {
  return (
    <Routes>
      <Route
        path="/"
        element={
          <main>
            <h1>ulearn</h1>
            <p>Turn YouTube playlists into structured, trackable study courses.</p>
          </main>
        }
      />
    </Routes>
  )
}

export default App
