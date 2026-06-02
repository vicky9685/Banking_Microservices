import { Routes, Route, Navigate, Link, useNavigate } from 'react-router-dom'
import { LayoutDashboard, ArrowLeftRight, Users, LogOut, Wallet, Menu, X, Landmark, ClipboardCheck } from 'lucide-react'
import { useState } from 'react'
import { getToken, setToken } from './api'
import Login from './pages/Login'
import Register from './pages/Register'
import Dashboard from './pages/Dashboard'
import Accounts from './pages/Accounts'
import TransferPage from './pages/Transfer'
import Customer from './pages/Customer'
import Loans from './pages/Loans'
import Tasks from './pages/Tasks'

function RequireAuth({ children }) {
  if (!getToken()) return <Navigate to="/login" replace />
  return children
}

function Shell({ children }) {
  const nav = useNavigate()
  const [open, setOpen] = useState(false)
  const logout = () => {
    setToken(null)
    nav('/login')
  }
  const NavLinks = () => (
    <>
      <NavLink to="/" icon={<LayoutDashboard size={18} />} label="Dashboard" />
      <NavLink to="/accounts" icon={<Wallet size={18} />} label="Accounts" />
      <NavLink to="/transfer" icon={<ArrowLeftRight size={18} />} label="Transfer" />
      <NavLink to="/loans" icon={<Landmark size={18} />} label="Loans" />
      <NavLink to="/tasks" icon={<ClipboardCheck size={18} />} label="My tasks" />
      <NavLink to="/customer" icon={<Users size={18} />} label="Profile" />
    </>
  )
  return (
    <div className="min-h-screen flex flex-col md:flex-row">
      <header className="md:hidden flex items-center justify-between bg-white border-b px-4 py-3">
        <div className="font-semibold text-brand-700">Banking Platform</div>
        <button onClick={() => setOpen((o) => !o)} className="p-2 rounded hover:bg-slate-100">
          {open ? <X size={20} /> : <Menu size={20} />}
        </button>
      </header>
      <aside
        className={`bg-white border-r w-full md:w-60 md:flex md:flex-col ${
          open ? 'flex flex-col' : 'hidden'
        }`}
      >
        <div className="hidden md:block px-5 py-5 text-xl font-semibold text-brand-700">
          Banking
        </div>
        <nav className="flex flex-col gap-1 px-2 py-2">
          <NavLinks />
          <button onClick={logout} className="btn-ghost mt-2 justify-start">
            <LogOut size={18} /> Sign out
          </button>
        </nav>
      </aside>
      <main className="flex-1 p-4 md:p-8 max-w-6xl mx-auto w-full">{children}</main>
    </div>
  )
}

function NavLink({ to, icon, label }) {
  return (
    <Link
      to={to}
      className="flex items-center gap-2 px-3 py-2 rounded-lg text-sm text-slate-700 hover:bg-brand-50 hover:text-brand-700"
    >
      {icon} {label}
    </Link>
  )
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route
        path="/*"
        element={
          <RequireAuth>
            <Shell>
              <Routes>
                <Route path="/" element={<Dashboard />} />
                <Route path="/accounts" element={<Accounts />} />
                <Route path="/transfer" element={<TransferPage />} />
                <Route path="/loans" element={<Loans />} />
                <Route path="/tasks" element={<Tasks />} />
                <Route path="/customer" element={<Customer />} />
                <Route path="*" element={<Navigate to="/" />} />
              </Routes>
            </Shell>
          </RequireAuth>
        }
      />
    </Routes>
  )
}
