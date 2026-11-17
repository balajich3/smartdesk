import { useEffect, useState } from 'react'
import { ArrowRight, BarChart3, Check, CircleAlert, Ticket, Users } from 'lucide-react'
import { Link } from 'react-router-dom'
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { toast } from 'sonner'
import { useTickets } from '../../features/tickets/useTickets.js'
import { useAuth } from '../../features/auth/useAuth.js'
import { ROLES } from '../../context/roles.js'
import api from '../../services/api.js'
import TicketCard from '../../features/tickets/TicketCard.jsx'

function ManagerDashboard() {
  const { tickets } = useTickets()
  const { user } = useAuth()
  const isAdmin = user.role === ROLES.ADMIN
  const basePath = isAdmin ? '/admin' : '/manager'
  const [stats, setStats] = useState(null)
  const [pendingAgents, setPendingAgents] = useState([])
  const [users, setUsers] = useState([])
  const [busyId, setBusyId] = useState(null)

  useEffect(() => {
    api.get('/api/manager/dashboard').then(({ data }) => setStats(data)).catch(() => {})
    if (user.role === ROLES.MANAGER) {
      api.get('/api/manager/agents/pending').then(({ data }) => setPendingAgents(data || [])).catch(() => {})
    }
    if (isAdmin) api.get('/api/admin/users').then(({ data }) => setUsers(data || [])).catch(() => {})
  }, [isAdmin, user.role])

  async function decideAgent(agentId, decision) {
    setBusyId(agentId)
    try {
      await api.patch(`/api/manager/agents/${agentId}/${decision}`)
      setPendingAgents((current) => current.filter((agent) => agent.id !== agentId))
      toast.success(decision === 'approve' ? 'Support agent approved' : 'Support agent rejected')
    } catch (error) {
      toast.error(error.response?.data?.message || `Unable to ${decision} agent`)
    } finally {
      setBusyId(null)
    }
  }

  const metrics = stats ? [
    { label: 'Total tickets', value: stats.totalTickets, icon: Ticket },
    { label: 'Open tickets', value: stats.openTickets, icon: CircleAlert },
    { label: 'Unassigned', value: stats.unassignedTickets, icon: Users },
    { label: 'In progress', value: stats.inProgressTickets, icon: CircleAlert },
  ] : [
    { label: 'Tickets loaded', value: tickets.length, icon: Ticket },
    { label: 'Open tickets', value: tickets.filter((t) => t.status === 'Open').length, icon: CircleAlert },
    { label: 'Resolved', value: tickets.filter((t) => t.status === 'Resolved').length, icon: Check },
    { label: 'Closed', value: tickets.filter((t) => t.status === 'Closed').length, icon: Users },
  ]

  const statusData = [
    ['Open', stats?.openTickets],
    ['In progress', stats?.inProgressTickets],
    ['Waiting', stats?.waitingForRequesterTickets],
    ['Resolved', stats?.resolvedTickets],
    ['Closed', stats?.closedTickets],
  ].map(([status, count]) => ({ status, count: count ?? tickets.filter((ticket) => ticket.status === status).length }))
  const recentTickets = [...tickets].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)).slice(0, 4)

  return (
      <section className="mx-auto w-full max-w-6xl px-6 py-9 sm:px-10 sm:py-11">
        <div className="flex flex-col justify-between gap-5 sm:flex-row sm:items-end">
          <div>
            <p className="text-sm font-medium text-neutral-500">{isAdmin ? 'Admin workspace' : `${user.department} manager workspace`}</p>
            <h1 className="mt-2 text-3xl font-semibold tracking-tight text-neutral-900">{isAdmin ? 'Admin overview' : 'Department overview'}</h1>
            <p className="mt-2 text-sm text-neutral-500">Monitor ticket activity and support operations available to your account.</p>
          </div>
          <Link className="inline-flex h-10 items-center gap-2 rounded-lg border border-neutral-200 bg-white px-4 text-sm font-medium text-neutral-700 hover:bg-neutral-50" to={`${basePath}/reports`}>
            <BarChart3 size={16} /> View reports
          </Link>
        </div>

        <div className="mt-8 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          {metrics.map(({ label, value, icon: Icon }) => (
              <div className="rounded-2xl border border-neutral-200 bg-white p-5" key={label}>
                <div className="flex items-center justify-between"><p className="text-sm text-neutral-500">{label}</p><Icon className="text-neutral-400" size={17} /></div>
                <p className="mt-5 text-3xl font-semibold tracking-tight text-neutral-900">{value ?? '—'}</p>
              </div>
          ))}
        </div>

        {isAdmin ? (
            <section className="mt-6 rounded-2xl border border-neutral-200 bg-white p-5">
              <h2 className="text-sm font-semibold text-neutral-900">Registered users</h2>
              <p className="mt-2 text-3xl font-semibold">{users.length}</p>
              <p className="mt-1 text-xs text-neutral-500">User administration is available in the team workspace.</p>
            </section>
        ) : (
            <section className="mt-6 overflow-hidden rounded-2xl border border-neutral-200 bg-white">
              <div className="flex items-center justify-between border-b border-neutral-100 px-5 py-4 sm:px-6">
                <div><h2 className="text-sm font-semibold text-neutral-900">Pending support-agent approvals</h2><p className="mt-1 text-xs text-neutral-500">Agents in your department waiting for approval.</p></div>
                <span className="rounded-full bg-neutral-100 px-2.5 py-1 text-xs">{pendingAgents.length} pending</span>
              </div>
              {pendingAgents.length ? pendingAgents.map((agent) => (
                  <div className="flex items-center justify-between gap-3 border-b border-neutral-100 px-5 py-4 last:border-0 sm:px-6" key={agent.id}>
                    <div><p className="text-sm font-medium text-neutral-900">{agent.fullName}</p><p className="mt-1 text-xs text-neutral-500">{agent.email} · {agent.department}</p></div>
                    <div className="flex gap-2">
                      <button disabled={busyId === agent.id} onClick={() => decideAgent(agent.id, 'approve')} className="inline-flex h-9 items-center gap-2 rounded-lg bg-neutral-900 px-3 text-xs font-medium text-white disabled:opacity-50"><Check size={14} /> Approve</button>
                      <button disabled={busyId === agent.id} onClick={() => decideAgent(agent.id, 'reject')} className="h-9 rounded-lg border border-neutral-200 px-3 text-xs font-medium text-neutral-700 disabled:opacity-50">Reject</button>
                    </div>
                  </div>
              )) : <p className="px-5 py-6 text-sm text-neutral-500">No pending agent approvals.</p>}
            </section>
        )}

        <div className="mt-5 grid gap-5 xl:grid-cols-[1.2fr_0.8fr]">
          <section className="rounded-2xl border border-neutral-200 bg-white p-5 sm:p-6">
            <h2 className="text-sm font-semibold text-neutral-900">Tickets by status</h2>
            <div className="mt-5 h-64 w-full">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={statusData} margin={{ top: 4, right: 10, left: -20, bottom: 4 }}>
                  <CartesianGrid stroke="#eeeeee" strokeDasharray="3 3" vertical={false} />
                  <XAxis axisLine={false} dataKey="status" tick={{ fill: '#737373', fontSize: 11 }} tickLine={false} />
                  <YAxis allowDecimals={false} axisLine={false} tick={{ fill: '#a3a3a3', fontSize: 11 }} tickLine={false} />
                  <Tooltip /><Bar dataKey="count" fill="#525252" radius={[5, 5, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </section>
          <section className="rounded-2xl border border-neutral-200 bg-white">
            <div className="flex items-center justify-between border-b border-neutral-100 px-5 py-4 sm:px-6"><h2 className="text-sm font-semibold text-neutral-900">Recent tickets</h2><Link className="text-xs text-neutral-600" to={`${basePath}/tickets`}>View all <ArrowRight size={13} className="inline" /></Link></div>
            <div className="divide-y divide-neutral-100">{recentTickets.map((ticket) => <TicketCard compact key={ticket.id} showRequester ticket={ticket} to={`${basePath}/tickets/${ticket.id}`} />)}</div>
          </section>
        </div>
      </section>
  )
}

export default ManagerDashboard
