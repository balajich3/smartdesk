import api from '../../services/api.js'

const pageData = data => Array.isArray(data) ? data : (data?.content || [])
const statusLabels = {
  OPEN: 'Open',
  ASSIGNED: 'Assigned',
  IN_PROGRESS: 'In progress',
  WAITING_FOR_REQUESTER: 'Waiting on employee',
  REQUESTER_APPROVED: 'Employee approved',
  RESOLVED: 'Resolved',
  CLOSED: 'Closed',
  CANCELLED: 'Withdrawn',
}
const statusValues = Object.fromEntries(Object.entries(statusLabels).map(([key, value]) => [value, key]))

export function mapTicket(ticket, user) {
  return {
    ...ticket,
    id: String(ticket.id),
    backendId: ticket.id,
    ticketNumber: ticket.ticketNumber,
    summary: ticket.aiSummary || '',
    department: ticket.department || user?.department || '',
    requesterEmail: ticket.requesterEmail || '',
    assignee: ticket.assignedAgent || null,
    status: statusLabels[ticket.status] || ticket.status,
    priority: String(ticket.priority || 'MEDIUM').toLowerCase().replace(/^./, c => c.toUpperCase()),
    category: ticket.category || '',
    activity: [{ author: ticket.requester || 'Requester', message: 'Ticket created', createdAt: ticket.createdAt }],
  }
}

export function toBackendStatus(status) { return statusValues[status] || status }

export async function loadTicketsForUser(user) {
  let responses = []
  if (user.role === 'EMPLOYEE') {
    const { data } = await api.get('/api/tickets/my', { params: { page: 0, size: 100 } })
    responses = pageData(data)
  } else if (user.role === 'SUPPORT_AGENT') {
    const [queue, assigned, mine] = await Promise.all([
      api.get('/api/agent/queue', { params: { page: 0, size: 100 } }),
      api.get('/api/agent/assigned', { params: { page: 0, size: 100 } }),
      api.get('/api/tickets/my', { params: { page: 0, size: 100 } }),
    ])
    // Queue contains only unassigned OPEN tickets; assigned contains only this agent's work.
    responses = [...pageData(queue.data), ...pageData(assigned.data), ...pageData(mine.data)]
  } else {
    const [departmentTickets, ownTickets] = await Promise.all([
      api.get('/api/manager/tickets', { params: { page: 0, size: 100 } }),
      api.get('/api/tickets/my', { params: { page: 0, size: 100 } }),
    ])
    responses = [...pageData(departmentTickets.data), ...pageData(ownTickets.data)]
  }
  const unique = new Map()
  responses.forEach(ticket => unique.set(String(ticket.id), mapTicket(ticket, user)))
  return [...unique.values()]
}

export async function getBackendTicket(ticketId) {
  const { data } = await api.get(`/api/tickets/${ticketId}`)
  return data
}

export async function createBackendTicket(values) {
  const { data } = await api.post('/api/tickets', {
    title: values.title.trim(),
    description: values.description.trim(),
  }, { headers: { 'Idempotency-Key': crypto.randomUUID() } })
  return data
}

export async function claimBackendTicket(ticketId) {
  return api.post(`/api/agent/tickets/${ticketId}/claim`)
}

export async function updateBackendTicketStatus(ticketId, status) {
  return api.patch(`/api/agent/tickets/${ticketId}/status`, { status: toBackendStatus(status) })
}

export async function acknowledgeBackendResolution(ticketId, accepted) {
  const { data } = await api.post(`/api/tickets/${ticketId}/acknowledgement`, null, { params: { accepted } })
  return data
}

export async function withdrawBackendTicket(ticketId) {
  const { data } = await api.post(`/api/tickets/${ticketId}/withdraw`)
  return data
}

export async function getBackendComments(ticketId) {
  const { data } = await api.get(`/api/tickets/${ticketId}/comments`)
  return data
}

export async function addBackendComment(ticketId, message) {
  const { data } = await api.post(`/api/tickets/${ticketId}/comments`, { message, internalNote: false })
  return data
}
