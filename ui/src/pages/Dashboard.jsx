import { useQuery } from '@tanstack/react-query'
import { accounts } from '../api'
import { getCustomerId } from '../auth'
import { Wallet, TrendingUp, ShieldCheck } from 'lucide-react'

export default function Dashboard() {
  const customerId = getCustomerId()
  const { data, isLoading } = useQuery({
    queryKey: ['accounts', customerId],
    queryFn: () => accounts.byCustomer(customerId),
    enabled: !!customerId,
  })

  const total = (data || []).reduce((s, a) => s + Number(a.balance), 0)
  const active = (data || []).filter((a) => a.status === 'ACTIVE').length

  return (
    <div>
      <h1 className="text-2xl font-semibold text-slate-800 mb-1">Overview</h1>
      <p className="text-slate-500 mb-6">Snapshot of your accounts and activity.</p>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 mb-6">
        <StatCard icon={<Wallet />} label="Total balance" value={fmt(total)} />
        <StatCard icon={<TrendingUp />} label="Accounts" value={(data || []).length} />
        <StatCard icon={<ShieldCheck />} label="Active" value={active} />
      </div>

      <div className="card">
        <h2 className="font-semibold mb-3">Recent accounts</h2>
        {isLoading && <div className="text-sm text-slate-500">Loading…</div>}
        {!isLoading && (data || []).length === 0 && (
          <div className="text-sm text-slate-500">No accounts yet — open one from the Accounts page.</div>
        )}
        <div className="divide-y">
          {(data || []).slice(0, 5).map((a) => (
            <div key={a.id} className="py-3 flex items-center justify-between">
              <div>
                <div className="font-medium">{a.accountNumber}</div>
                <div className="text-xs text-slate-500">
                  {a.type} • {a.currency}
                </div>
              </div>
              <div className="text-right">
                <div className="font-semibold">{fmt(a.balance)}</div>
                <span
                  className={`badge ${
                    a.status === 'ACTIVE' ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-600'
                  }`}
                >
                  {a.status}
                </span>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

function StatCard({ icon, label, value }) {
  return (
    <div className="card flex items-center gap-4">
      <div className="rounded-lg bg-brand-50 text-brand-600 p-3">{icon}</div>
      <div>
        <div className="text-sm text-slate-500">{label}</div>
        <div className="text-xl font-semibold">{value}</div>
      </div>
    </div>
  )
}

function fmt(n) {
  return new Intl.NumberFormat(undefined, { style: 'currency', currency: 'USD' }).format(Number(n) || 0)
}
