import { useCallback, useEffect, useState } from 'react'
import { TicketContext } from './ticketStore.js'
import { useAuth } from '../auth/useAuth.js'
import {
  acknowledgeBackendResolution,
  addBackendComment,
  getBackendComments,
  getBackendTicket,
  claimBackendTicket,
  createBackendTicket,
  loadTicketsForUser,
  mapTicket,
  updateBackendTicketStatus,
  withdrawBackendTicket,
} from './ticketApi.js'

export function TicketProvider({ children }) {
  const { user } = useAuth()
  const [tickets, setTickets] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const refreshTickets = useCallback(async () => {
    if (!user) { setTickets([]); return }
    setLoading(true)
    setError('')
    try {
      setTickets(await loadTicketsForUser(user))
    } catch (err) {
      setError(err.response?.data?.message || 'Unable to load tickets from the server.')
    } finally {
      setLoading(false)
    }
  }, [user])

  useEffect(() => { refreshTickets() }, [refreshTickets])

  async function createTicket(formValues) {
    const response = await createBackendTicket(formValues)
    const ticket = mapTicket(response, user)
    setTickets(current => [ticket, ...current.filter(item => item.id !== ticket.id)])
    return ticket
  }

  async function updateTicket(ticketId, updates) {
    if (updates.status) await updateBackendTicketStatus(ticketId, updates.status)
    await refreshTickets()
  }

  async function claimTicket(ticketId) {
    await claimBackendTicket(ticketId)
    await refreshTickets()
  }

  async function acknowledgeResolution(ticketId, accepted) {
    const response = await acknowledgeBackendResolution(ticketId, accepted)
    const updated = mapTicket(response, user)
    setTickets(current => current.map(ticket => ticket.id === String(ticketId) ? updated : ticket))
    await refreshTickets()
    return updated
  }

  async function getTicket(ticketId) {
    const data = await getBackendTicket(ticketId)
    const ticket = mapTicket(data, user)
    setTickets(current => [ticket, ...current.filter(item => item.id !== ticket.id)])
    return ticket
  }

  async function withdrawTicket(ticketId) {
    const data = await withdrawBackendTicket(ticketId)
    const ticket = mapTicket(data, user)
    setTickets(current => current.map(item => item.id === ticket.id ? ticket : item))
    await refreshTickets()
    return ticket
  }

  async function getComments(ticketId) { return getBackendComments(ticketId) }
  async function addComment(ticketId, message) { return addBackendComment(ticketId, message) }

  const value = {
    tickets, loading, error, refreshTickets, createTicket, updateTicket,
    claimTicket, acknowledgeResolution, getTicket, withdrawTicket, getComments, addComment,
  }
  return <TicketContext.Provider value={value}>{children}</TicketContext.Provider>
}
