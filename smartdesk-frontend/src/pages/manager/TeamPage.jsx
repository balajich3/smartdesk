import { useCallback, useEffect, useState } from 'react'
import { Check, ShieldCheck, Users, X } from 'lucide-react'
import { toast } from 'sonner'
import { ROLES } from '../../context/roles.js'
import { useAuth } from '../../features/auth/useAuth.js'
import api from '../../services/api.js'

function TeamPage() {
    const { user } = useAuth()
    const isAdmin = user.role === ROLES.ADMIN
    const [members, setMembers] = useState([])
    const [pending, setPending] = useState([])
    const [departments, setDepartments] = useState([])
    const [loading, setLoading] = useState(true)
    const [workingId, setWorkingId] = useState(null)

    const load = useCallback(async () => {
        setLoading(true)
        try {
            if (isAdmin) {
                const [usersResponse, pendingResponse, departmentsResponse] = await Promise.all([
                    api.get('/api/admin/users'),
                    api.get('/api/admin/approvals/pending'),
                    api.get('/api/admin/departments'),
                ])
                setMembers(usersResponse.data || [])
                setPending(pendingResponse.data || [])
                setDepartments(departmentsResponse.data || [])
            } else {
                const { data } = await api.get('/api/manager/agents/pending')
                setPending(data || [])
            }
        } catch (error) {
            toast.error(error.response?.data?.message || 'Unable to load team data')
        } finally { setLoading(false) }
    }, [isAdmin])

    useEffect(() => { load() }, [load])

    async function decide(userId, decision) {
        setWorkingId(userId)
        try {
            await api.patch(`/api/admin/approvals/${userId}/${decision}`)
            toast.success(decision === 'approve' ? 'Account approved. Approval email sent to the user.' : 'Registration rejected.')
            setPending(current => current.filter(item => item.id !== userId))
            setMembers(current => current.map(item => item.id === userId ? { ...item, approvalStatus: decision === 'approve' ? 'APPROVED' : 'REJECTED' } : item))
        } catch (error) { toast.error(error.response?.data?.message || 'Unable to update approval') }
        finally { setWorkingId(null) }
    }

    async function updateDepartment(member, departmentCode) {
        try {
            const { data } = await api.patch(`/api/admin/users/${member.id}/department`, { departmentCode })
            setMembers(current => current.map(item => item.id === member.id ? data : item))
            toast.success(`${member.fullName}'s department updated`)
        } catch (error) {
            toast.error(error.response?.data?.message || 'Unable to update department')
        }
    }

    async function updateActive(member, active) {
        try {
            await api.patch(`/api/admin/users/${member.id}/status`, { active })
            setMembers(current => current.map(item => item.id === member.id ? { ...item, active } : item))
            toast.success('User status updated')
        } catch (error) { toast.error(error.response?.data?.message || 'Unable to update user') }
    }

    return <section className="mx-auto w-full max-w-5xl px-6 py-9 sm:px-10 sm:py-11">
        <p className="text-sm font-medium text-neutral-500">{isAdmin ? 'Admin workspace' : `${user.department} manager workspace`}</p>
        <h1 className="mt-2 text-3xl font-semibold tracking-tight text-neutral-900">{isAdmin ? 'User administration & approvals' : 'Pending agent approvals'}</h1>
        <p className="mt-2 text-sm text-neutral-500">{isAdmin ? 'Review manager and support-agent registrations before granting workspace access.' : 'Review support agents registered in your department.'}</p>
        <section className="mt-7 overflow-hidden rounded-2xl border border-neutral-200 bg-white">
            <div className="flex items-center gap-2 border-b border-neutral-100 px-5 py-4"><ShieldCheck size={17}/><h2 className="text-sm font-semibold">Pending approvals ({pending.length})</h2></div>
            {loading ? <p className="p-6 text-sm text-neutral-500">Loading…</p> : pending.length ? pending.map(member => <div key={member.id} className="flex flex-col gap-3 border-b border-neutral-100 px-5 py-4 last:border-0 sm:flex-row sm:items-center sm:justify-between"><div><p className="text-sm font-medium text-neutral-900">{member.fullName}</p><p className="mt-1 text-xs text-neutral-500">{member.email} · {member.department || 'No department'} · {[...(member.roles || [])].join(', ')}</p><p className="mt-1 text-xs text-amber-700">Awaiting administrator approval</p></div>{isAdmin && <div className="flex gap-2"><button disabled={workingId === member.id} onClick={() => decide(member.id, 'approve')} className="inline-flex items-center gap-1 rounded-lg bg-emerald-600 px-3 py-2 text-xs font-medium text-white disabled:opacity-50"><Check size={14}/>Approve</button><button disabled={workingId === member.id} onClick={() => decide(member.id, 'reject')} className="inline-flex items-center gap-1 rounded-lg border border-red-200 px-3 py-2 text-xs font-medium text-red-700 disabled:opacity-50"><X size={14}/>Reject</button></div>}{!isAdmin && <span className="text-xs text-neutral-500">Admin approval required</span>}</div>) : <p className="p-6 text-sm text-neutral-500">No registrations are waiting for approval.</p>}
        </section>
        {isAdmin && <section className="mt-7 overflow-hidden rounded-2xl border border-neutral-200 bg-white"><div className="flex items-center gap-2 border-b border-neutral-100 px-5 py-4"><Users size={17}/><h2 className="text-sm font-semibold">All users ({members.length})</h2></div>{members.map(member => {
            const canReassign = member.approvalStatus === 'APPROVED' && (member.roles?.includes('MANAGER') || member.roles?.includes('SUPPORT_AGENT'))
            const selectedDepartment = departments.find(dept => dept.name === member.department)?.code || ''
            return <div key={member.id} className="flex flex-col gap-3 border-b border-neutral-100 px-5 py-4 last:border-0 sm:flex-row sm:items-center sm:justify-between">
                <div className="min-w-0">
                    <p className="text-sm font-medium text-neutral-900">{member.fullName}</p>
                    <p className="mt-1 text-xs text-neutral-500">{member.email} · {[...(member.roles || [])].join(', ')}</p>
                    <p className="mt-1 text-xs text-neutral-500">Approval: {member.approvalStatus || 'NOT_REQUIRED'} · Account: {member.active ? 'Active' : 'Disabled'}</p>
                </div>
                <div className="flex flex-wrap items-center gap-2">
                    {canReassign && <select aria-label={`Department for ${member.fullName}`} value={selectedDepartment} onChange={event => updateDepartment(member, event.target.value)} className="h-9 rounded-lg border border-neutral-200 bg-white px-2 text-xs">
                        {departments.map(dept => <option key={dept.code} value={dept.code}>{dept.name}</option>)}
                    </select>}
                    {!member.roles?.includes('ADMIN') && <button onClick={() => updateActive(member, !member.active)} className={`rounded-lg px-3 py-2 text-xs font-medium ${member.active ? 'border border-neutral-200 text-neutral-700' : 'bg-neutral-900 text-white'}`}>{member.active ? 'Disable account' : 'Enable account'}</button>}
                </div>
            </div>
        })}</section>}
    </section>
}
export default TeamPage
