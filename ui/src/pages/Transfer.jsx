import { useState } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { accounts, transfers } from '../api'
import { getCustomerId } from '../auth'
import { ArrowRight, CheckCircle2, AlertTriangle } from 'lucide-react'

export default function TransferPage() {
  const customerId = getCustomerId()
  const { data: myAccounts } = useQuery({
    queryKey: ['accounts', customerId],
    queryFn: () => accounts.byCustomer(customerId),
    enabled: !!customerId,
  })

  const [form, setForm] = useState({ fromAccountId: '', toAccountId: '', amount: '', currency: 'USD' })
  const [result, setResult] = useState(null)

  const m = useMutation({
    mutationFn: () =>
      transfers.initiate(
        { ...form, amount: Number(form.amount) },
        crypto.randomUUID(),
      ),
    onSuccess: (r) => setResult({ ok: true, data: r }),
    onError: (e) => setResult({ ok: false, message: e.response?.data?.message || 'Transfer failed' }),
  })

  return (
    <div className="max-w-2xl">
      <h1 className="text-2xl font-semibold mb-1">Transfer money</h1>
      <p className="text-slate-500 text-sm mb-6">
        Transfers run through a Saga; status updates as participants debit and credit.
      </p>
      <div className="card space-y-3">
        <label className="block">
          <span className="text-sm">From account</span>
          <select className="input mt-1" value={form.fromAccountId} onChange={(e) => setForm({ ...form, fromAccountId: e.target.value })}>
            <option value="">Select…</option>
            {(myAccounts || []).map((a) => (
              <option key={a.id} value={a.id}>
                {a.accountNumber} ({a.currency} {a.balance})
              </option>
            ))}
          </select>
        </label>
        <label className="block">
          <span className="text-sm">To account ID</span>
          <input className="input mt-1" placeholder="UUID of recipient account"
                 value={form.toAccountId}
                 onChange={(e) => setForm({ ...form, toAccountId: e.target.value })} />
        </label>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <label className="block">
            <span className="text-sm">Amount</span>
            <input className="input mt-1" type="number" min="0.01" step="0.01"
                   value={form.amount}
                   onChange={(e) => setForm({ ...form, amount: e.target.value })} />
          </label>
          <label className="block">
            <span className="text-sm">Currency</span>
            <input className="input mt-1" value={form.currency} onChange={(e) => setForm({ ...form, currency: e.target.value })} />
          </label>
        </div>
        <button
          className="btn-primary w-full sm:w-auto"
          disabled={m.isPending || !form.fromAccountId || !form.toAccountId || !form.amount}
          onClick={() => m.mutate()}
        >
          {m.isPending ? 'Submitting…' : <>Send <ArrowRight size={16} /></>}
        </button>
      </div>

      {result && (
        <div className={`card mt-4 ${result.ok ? 'border-emerald-200' : 'border-red-200'}`}>
          {result.ok ? (
            <div className="flex items-start gap-3 text-emerald-700">
              <CheckCircle2 />
              <div>
                <div className="font-medium">Transfer accepted</div>
                <div className="text-xs text-slate-600 break-all">id: {result.data.id} · status: {result.data.status}</div>
              </div>
            </div>
          ) : (
            <div className="flex items-start gap-3 text-red-700">
              <AlertTriangle />
              <div>
                <div className="font-medium">Transfer failed</div>
                <div className="text-sm">{result.message}</div>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
