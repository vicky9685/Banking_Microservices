import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { auth, setToken } from '../api'
import { Loader2 } from 'lucide-react'

export default function Login() {
  const nav = useNavigate()
  const [form, setForm] = useState({ username: '', password: '' })
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      const { accessToken } = await auth.login(form)
      setToken(accessToken)
      nav('/')
    } catch (err) {
      setError(err.response?.data?.message || 'Login failed')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="min-h-screen grid place-items-center bg-gradient-to-br from-brand-50 to-white px-4">
      <div className="w-full max-w-md card">
        <h1 className="text-2xl font-semibold text-brand-700">Welcome back</h1>
        <p className="text-sm text-slate-500 mb-5">Sign in to your banking dashboard.</p>
        <form onSubmit={submit} className="space-y-3">
          <label className="block">
            <span className="text-sm text-slate-700">Username</span>
            <input
              className="input mt-1"
              value={form.username}
              onChange={(e) => setForm({ ...form, username: e.target.value })}
              required
            />
          </label>
          <label className="block">
            <span className="text-sm text-slate-700">Password</span>
            <input
              type="password"
              className="input mt-1"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              required
            />
          </label>
          {error && <div className="text-sm text-red-600">{error}</div>}
          <button className="btn-primary w-full" disabled={busy}>
            {busy ? <Loader2 className="animate-spin" size={16} /> : 'Sign in'}
          </button>
        </form>
        <div className="mt-4 text-sm text-slate-600">
          New here?{' '}
          <Link to="/register" className="text-brand-600 hover:underline">
            Create an account
          </Link>
        </div>
      </div>
    </div>
  )
}
