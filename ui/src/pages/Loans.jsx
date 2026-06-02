import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { workflows } from '../api'
import { getCustomerId } from '../auth'
import { CheckCircle2, AlertTriangle } from 'lucide-react'

export default function Loans() {
  const customerId = getCustomerId()
  const [form, setForm] = useState({ amount: 5000, currency: 'USD', termMonths: 36 })
  const [result, setResult] = useState(null)

  const apply = useMutation({
    mutationFn: () =>
      workflows.startLoan({
        customerId,
        amount: Number(form.amount),
        currency: form.currency,
        termMonths: Number(form.termMonths),
      }),
    onSuccess: (r) => setResult({ ok: true, data: r }),
    onError: (e) => setResult({ ok: false, message: e.response?.data?.message || 'Application failed' }),
  })

  return (
    <div className="max-w-xl">
      <h1 className="text-2xl font-semibold mb-1">Apply for a loan</h1>
      <p className="text-slate-500 text-sm mb-6">
        Runs through Camunda: credit check → DMN decision → auto-approve or underwriter review → disbursement.
      </p>
      <div className="card space-y-3">
        <label className="block">
          <span className="text-sm">Amount</span>
          <input
            className="input mt-1" type="number" min="100"
            value={form.amount}
            onChange={(e) => setForm({ ...form, amount: e.target.value })}
          />
        </label>
        <div className="grid grid-cols-2 gap-3">
          <label className="block">
            <span className="text-sm">Currency</span>
            <input className="input mt-1" value={form.currency}
                   onChange={(e) => setForm({ ...form, currency: e.target.value })} />
          </label>
          <label className="block">
            <span className="text-sm">Term (months)</span>
            <input className="input mt-1" type="number"
                   value={form.termMonths}
                   onChange={(e) => setForm({ ...form, termMonths: e.target.value })} />
          </label>
        </div>
        <button className="btn-primary" onClick={() => apply.mutate()} disabled={apply.isPending || !customerId}>
          {apply.isPending ? 'Submitting…' : 'Submit application'}
        </button>
        {!customerId && (
          <div className="text-xs text-amber-700">Create a customer profile first under Profile.</div>
        )}
      </div>
      {result && (
        <div className={`card mt-4 ${result.ok ? 'border-emerald-200' : 'border-red-200'}`}>
          {result.ok ? (
            <div className="flex items-start gap-3 text-emerald-700">
              <CheckCircle2 />
              <div>
                <div className="font-medium">Application started</div>
                <div className="text-xs text-slate-600 break-all">processInstanceId: {result.data.processInstanceId}</div>
              </div>
            </div>
          ) : (
            <div className="flex items-start gap-3 text-red-700">
              <AlertTriangle />
              <div>
                <div className="font-medium">Application failed</div>
                <div className="text-sm">{result.message}</div>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
