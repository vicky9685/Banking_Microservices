import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { customers } from '../api'
import { getCustomerId, setCustomerId } from '../auth'

export default function Customer() {
  const customerId = getCustomerId()
  const qc = useQueryClient()
  const [creating, setCreating] = useState(!customerId)

  const { data } = useQuery({
    queryKey: ['customer', customerId],
    queryFn: () => customers.get(customerId),
    enabled: !!customerId,
  })

  const [form, setForm] = useState({
    firstName: '', lastName: '', email: '', phone: '', nationalId: '', dateOfBirth: '',
  })
  const create = useMutation({
    mutationFn: () => customers.create(form),
    onSuccess: (c) => {
      setCustomerId(c.id)
      setCreating(false)
      qc.invalidateQueries({ queryKey: ['customer'] })
    },
  })
  const verify = useMutation({
    mutationFn: () => customers.setKyc(customerId, 'VERIFIED'),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['customer'] }),
  })

  if (creating || !customerId) {
    return (
      <div>
        <h1 className="text-2xl font-semibold mb-4">Onboard customer</h1>
        <div className="card space-y-3 max-w-2xl">
          {['firstName','lastName','email','phone','nationalId','dateOfBirth'].map((f) => (
            <label className="block" key={f}>
              <span className="text-sm capitalize">{f.replace(/([A-Z])/g, ' $1')}</span>
              <input
                className="input mt-1"
                type={f === 'dateOfBirth' ? 'date' : f === 'email' ? 'email' : 'text'}
                value={form[f]}
                onChange={(e) => setForm({ ...form, [f]: e.target.value })}
              />
            </label>
          ))}
          {create.isError && <div className="text-sm text-red-600">{create.error?.response?.data?.message}</div>}
          <button className="btn-primary" onClick={() => create.mutate()} disabled={create.isPending}>
            {create.isPending ? 'Submitting…' : 'Create customer'}
          </button>
        </div>
      </div>
    )
  }

  return (
    <div>
      <h1 className="text-2xl font-semibold mb-4">Profile</h1>
      <div className="card max-w-2xl space-y-2">
        <Row k="Name" v={data ? `${data.firstName} ${data.lastName}` : '—'} />
        <Row k="Email" v={data?.email} />
        <Row k="Phone" v={data?.phone} />
        <Row k="National ID" v={data?.nationalId} />
        <Row k="KYC" v={
          <span className={`badge ${data?.kycStatus === 'VERIFIED' ? 'bg-emerald-50 text-emerald-700' : 'bg-amber-50 text-amber-700'}`}>
            {data?.kycStatus}
          </span>
        } />
        {data?.kycStatus !== 'VERIFIED' && (
          <button className="btn-primary mt-3" onClick={() => verify.mutate()} disabled={verify.isPending}>
            {verify.isPending ? 'Verifying…' : 'Mark KYC verified (back-office)'}
          </button>
        )}
      </div>
    </div>
  )
}

function Row({ k, v }) {
  return (
    <div className="flex justify-between border-b py-2 text-sm">
      <span className="text-slate-500">{k}</span>
      <span className="font-medium">{v || '—'}</span>
    </div>
  )
}
