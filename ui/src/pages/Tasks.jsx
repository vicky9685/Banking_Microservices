import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { workflows } from '../api'
import { CheckCircle2, XCircle } from 'lucide-react'

export default function Tasks() {
  const qc = useQueryClient()
  const { data, isLoading } = useQuery({
    queryKey: ['tasks'],
    queryFn: () => workflows.listTasks(),
    refetchInterval: 5000,
  })

  return (
    <div>
      <h1 className="text-2xl font-semibold mb-1">My tasks</h1>
      <p className="text-slate-500 text-sm mb-6">
        Approvals and underwriter reviews assigned by Camunda land here.
      </p>
      {isLoading && <div className="text-sm text-slate-500">Loading…</div>}
      {!isLoading && (data || []).length === 0 && (
        <div className="card text-sm text-slate-500">No pending tasks.</div>
      )}
      <div className="space-y-3">
        {(data || []).map((t) => (
          <TaskRow key={t.id} task={t} onChanged={() => qc.invalidateQueries({ queryKey: ['tasks'] })} />
        ))}
      </div>
    </div>
  )
}

function TaskRow({ task, onChanged }) {
  const approve = useMutation({
    mutationFn: () => workflows.complete(task.id, { approved: true, comment: 'Approved via UI' }),
    onSuccess: onChanged,
  })
  const reject = useMutation({
    mutationFn: () => workflows.complete(task.id, { approved: false, comment: 'Rejected via UI' }),
    onSuccess: onChanged,
  })
  return (
    <div className="card">
      <div className="flex items-start justify-between gap-3">
        <div>
          <div className="font-medium">{task.name}</div>
          <div className="text-xs text-slate-500">processInstanceId: {task.processInstanceId}</div>
          <pre className="mt-2 text-xs bg-slate-50 p-2 rounded border overflow-auto max-w-full">
{JSON.stringify(task.variables, null, 2)}
          </pre>
        </div>
        <div className="flex gap-2 shrink-0">
          <button className="btn-ghost border text-emerald-700" onClick={() => approve.mutate()} disabled={approve.isPending}>
            <CheckCircle2 size={16} /> Approve
          </button>
          <button className="btn-ghost border text-red-700" onClick={() => reject.mutate()} disabled={reject.isPending}>
            <XCircle size={16} /> Reject
          </button>
        </div>
      </div>
    </div>
  )
}
