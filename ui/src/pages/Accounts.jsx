import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { accounts } from '../api'
import { getCustomerId } from '../auth'
import { Plus, ArrowDownToLine, ArrowUpFromLine } from 'lucide-react'

export default function Accounts() {
  const customerId = getCustomerId()
  const qc = useQueryClient()
  const { data, isLoading } = useQuery({
    queryKey: ['accounts', customerId],
    queryFn: () => accounts.byCustomer(customerId),
    enabled: !!customerId,
  })
  const [showOpen, setShowOpen] = useState(false)

  return (
    <div>
      <div className="flex items-center justify-between mb-4">
        <div>
          <h1 className="text-2xl font-semibold">Accounts</h1>
          <p className="text-slate-500 text-sm">Manage your savings and checking accounts.</p>
        </div>
        <button className="btn-primary" onClick={() => setShowOpen(true)}>
          <Plus size={16} /> Open account
        </button>
      </div>
      {isLoading && <div className="text-sm text-slate-500">Loading…</div>}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        {(data || []).map((a) => (
          <AccountCard key={a.id} account={a} onChanged={() => qc.invalidateQueries({ queryKey: ['accounts'] })} />
        ))}
      </div>
      {showOpen && (
        <OpenAccountModal
          onClose={() => setShowOpen(false)}
          onCreated={() => {
            setShowOpen(false)
            qc.invalidateQueries({ queryKey: ['accounts'] })
          }}
          customerId={customerId}
        />
      )}
    </div>
  )
}

function AccountCard({ account, onChanged }) {
  const [amount, setAmount] = useState('')
  const dep = useMutation({
    mutationFn: () => accounts.deposit(account.id, Number(amount), 'UI deposit'),
    onSuccess: () => { setAmount(''); onChanged() },
  })
  const wd = useMutation({
    mutationFn: () => accounts.withdraw(account.id, Number(amount), 'UI withdraw'),
    onSuccess: () => { setAmount(''); onChanged() },
  })
  return (
    <div className="card">
      <div className="flex items-start justify-between">
        <div>
          <div className="text-sm text-slate-500">{account.type}</div>
          <div className="font-semibold tracking-wide">{account.accountNumber}</div>
        </div>
        <span className={`badge ${account.status === 'ACTIVE' ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-600'}`}>{account.status}</span>
      </div>
      <div className="mt-4 text-2xl font-semibold">
        {new Intl.NumberFormat(undefined, { style: 'currency', currency: account.currency || 'USD' }).format(Number(account.balance))}
      </div>
      <div className="mt-4 flex gap-2">
        <input
          className="input flex-1"
          placeholder="Amount"
          inputMode="decimal"
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
        />
        <button className="btn-ghost border" onClick={() => dep.mutate()} disabled={!amount || dep.isPending}>
          <ArrowDownToLine size={16} /> Deposit
        </button>
        <button className="btn-ghost border" onClick={() => wd.mutate()} disabled={!amount || wd.isPending}>
          <ArrowUpFromLine size={16} /> Withdraw
        </button>
      </div>
      {(dep.isError || wd.isError) && (
        <div className="text-sm text-red-600 mt-2">
          {(dep.error || wd.error)?.response?.data?.message || 'Operation failed'}
        </div>
      )}
    </div>
  )
}

function OpenAccountModal({ onClose, onCreated, customerId }) {
  const [form, setForm] = useState({ type: 'SAVINGS', currency: 'USD', initialDeposit: 0 })
  const m = useMutation({
    mutationFn: () => accounts.open({ ...form, customerId, initialDeposit: Number(form.initialDeposit) }),
    onSuccess: onCreated,
  })
  return (
    <div className="fixed inset-0 bg-black/40 grid place-items-center p-4 z-50">
      <div className="card w-full max-w-md">
        <h2 className="text-lg font-semibold mb-4">Open new account</h2>
        <div className="space-y-3">
          <label className="block">
            <span className="text-sm">Type</span>
            <select className="input mt-1" value={form.type} onChange={(e) => setForm({ ...form, type: e.target.value })}>
              {['SAVINGS', 'CHECKING', 'LOAN', 'CREDIT'].map((t) => <option key={t}>{t}</option>)}
            </select>
          </label>
          <label className="block">
            <span className="text-sm">Currency</span>
            <input className="input mt-1" value={form.currency} onChange={(e) => setForm({ ...form, currency: e.target.value })} />
          </label>
          <label className="block">
            <span className="text-sm">Initial deposit</span>
            <input className="input mt-1" type="number" min="0" value={form.initialDeposit} onChange={(e) => setForm({ ...form, initialDeposit: e.target.value })} />
          </label>
          {m.isError && <div className="text-sm text-red-600">{m.error?.response?.data?.message || 'Failed'}</div>}
          <div className="flex gap-2 justify-end pt-2">
            <button className="btn-ghost" onClick={onClose}>Cancel</button>
            <button className="btn-primary" onClick={() => m.mutate()} disabled={m.isPending}>
              {m.isPending ? 'Opening…' : 'Open account'}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
