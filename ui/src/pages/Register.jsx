import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { auth } from '../api'

export default function Register() {
  const nav = useNavigate()
  const [form, setForm] = useState({ username: '', password: '', email: '' })
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      await auth.register(form)
      nav('/login')
    } catch (err) {
      setError(err.response?.data?.message || 'Registration failed')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="min-h-screen grid place-items-center bg-gradient-to-br from-brand-50 to-white px-4">
      <div className="w-full max-w-md card">
        <h1 className="text-2xl font-semibold text-brand-700">Create account</h1>
        <p className="text-sm text-slate-500 mb-5">Set up access to the platform.</p>
        <form onSubmit={submit} className="space-y-3">
          {['username', 'email', 'password'].map((f) => (
            <label key={f} className="block">
              <span className="text-sm text-slate-700 capitalize">{f}</span>
              <input
                type={f === 'password' ? 'password' : f === 'email' ? 'email' : 'text'}
                className="input mt-1"
                value={form[f]}
                onChange={(e) => setForm({ ...form, [f]: e.target.value })}
                required
              />
            </label>
          ))}
          {error && <div className="text-sm text-red-600">{error}</div>}
          <button className="btn-primary w-full" disabled={busy}>
            {busy ? 'Registering…' : 'Create account'}
          </button>
        </form>
        <div className="mt-4 text-sm text-slate-600">
          Already have an account?{' '}
          <Link to="/login" className="text-brand-600 hover:underline">
            Sign in
          </Link>
        </div>
      </div>
    </div>
  )
}
