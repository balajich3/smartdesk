import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react'

import { toast } from 'sonner'
import { useAuth } from '../auth/useAuth.js'
import notificationApi from '../../services/notificationApi.js'
import { NotificationContext } from './notificationContext.js'

function mapNotification(item) {
  return {
    id: item.id,
    title: item.subject || item.type || 'SmartDesk update',
    message: item.message || '',
    createdAt: item.createdAt,
    isRead: Boolean(item.read),
    type: item.type,
  }
}

export function NotificationProvider({ children }) {
  const { user } = useAuth()

  const [notifications, setNotifications] = useState([])
  const [unreadCount, setUnreadCount] = useState(0)

  const seenIds = useRef(new Set())

  // Fetch notifications and unread count
  const refresh = useCallback(async () => {
    if (!user) {
      setNotifications([])
      setUnreadCount(0)
      seenIds.current.clear()
      return
    }

    try {
      const [listResponse, countResponse] = await Promise.all([
        notificationApi.get('/api/notifications'),
        notificationApi.get('/api/notifications/unread-count'),
      ])

      const items = (
          Array.isArray(listResponse.data) ? listResponse.data : []
      ).map(mapNotification)

      setNotifications(items)
      setUnreadCount(countResponse.data?.unreadCount || 0)

      seenIds.current = new Set(items.map((item) => item.id))
    } catch {
      // Keep the last successful data if Notification Service is unavailable.
    }
  }, [user])

  // Initial fetch and periodic refresh
  useEffect(() => {
    if (!user) {
      setNotifications([])
      setUnreadCount(0)
      seenIds.current.clear()
      return undefined
    }

    refresh()

    const timer = window.setInterval(refresh, 30000)

    return () => window.clearInterval(timer)
  }, [user, refresh])

  // WebSocket connection using STOMP over native WebSocket
  useEffect(() => {
    if (!user) return undefined

    const token = localStorage.getItem('accessToken')

    if (!token) return undefined

    let socket
    let reconnectTimer
    let heartbeatTimer
    let stopped = false
    let frameBuffer = ''

    function connect() {
      if (stopped) return

      // Local development uses localhost:8083.
      // Docker production uses the Nginx WebSocket proxy.
      const wsUrl =
          import.meta.env.VITE_NOTIFICATION_WS_URL ||
          (import.meta.env.PROD
              ? `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}/notification-service/ws-notifications`
              : 'ws://localhost:8083/ws-notifications')

      socket = new WebSocket(wsUrl)

      socket.onopen = () => {
        socket.send(
            `CONNECT
accept-version:1.2
heart-beat:10000,10000
Authorization:Bearer ${token}

\u0000`
        )
      }

      socket.onmessage = (event) => {
        frameBuffer += event.data

        const frames = frameBuffer.split('\u0000')
        frameBuffer = frames.pop() || ''

        for (const rawFrame of frames) {
          const frame = rawFrame.trimStart()

          if (frame.startsWith('CONNECTED')) {
            socket.send(
                'SUBSCRIBE\nid:smartdesk-notifications\ndestination:/user/queue/notifications\nack:auto\n\n\u0000'
            )

            heartbeatTimer = window.setInterval(() => {
              if (socket?.readyState === WebSocket.OPEN) {
                socket.send('\n')
              }
            }, 10000)
          } else if (frame.startsWith('MESSAGE')) {
            const body = frame.slice(frame.indexOf('\n\n') + 2)

            try {
              const incoming = mapNotification(JSON.parse(body))

              if (!seenIds.current.has(incoming.id)) {
                seenIds.current.add(incoming.id)

                if (!incoming.isRead) {
                  setUnreadCount((count) => count + 1)
                }

                setNotifications((current) => [
                  incoming,
                  ...current,
                ])

                toast(incoming.title, {
                  description: incoming.message,
                })
              }
            } catch {
              // Ignore malformed broker messages.
            }
          } else if (frame.startsWith('ERROR')) {
            socket.close()
          }
        }
      }

      socket.onclose = () => {
        window.clearInterval(heartbeatTimer)

        if (!stopped) {
          reconnectTimer = window.setTimeout(connect, 5000)
        }
      }

      socket.onerror = () => {
        socket.close()
      }
    }

    connect()

    return () => {
      stopped = true

      window.clearTimeout(reconnectTimer)
      window.clearInterval(heartbeatTimer)

      if (
          socket?.readyState === WebSocket.OPEN ||
          socket?.readyState === WebSocket.CONNECTING
      ) {
        socket.close()
      }
    }
  }, [user])

  // Mark one notification as read
  const markRead = useCallback(
      async (id) => {
        try {
          await notificationApi.patch(
              `/api/notifications/${id}/read`
          )

          const existing = notifications.find(
              (item) => item.id === id
          )

          if (existing && !existing.isRead) {
            setUnreadCount((count) => Math.max(0, count - 1))
          }

          setNotifications((current) =>
              current.map((item) =>
                  item.id === id
                      ? { ...item, isRead: true }
                      : item
              )
          )
        } catch {
          toast.error('Unable to mark notification as read')
        }
      },
      [notifications]
  )

  // Mark all notifications as read
  const markAllRead = useCallback(async () => {
    const unread = notifications.filter(
        (item) => !item.isRead
    )

    try {
      await Promise.all(
          unread.map((item) =>
              notificationApi.patch(
                  `/api/notifications/${item.id}/read`
              )
          )
      )

      setNotifications((current) =>
          current.map((item) => ({
            ...item,
            isRead: true,
          }))
      )

      const { data } = await notificationApi.get(
          '/api/notifications/unread-count'
      )

      setUnreadCount(data?.unreadCount || 0)
    } catch {
      toast.error('Unable to mark all notifications as read')
    }
  }, [notifications])

  const value = useMemo(
      () => ({
        notifications,
        markRead,
        markAllRead,
        unreadCount,
      }),
      [notifications, unreadCount, markRead, markAllRead]
  )

  return (
      <NotificationContext.Provider value={value}>
        {children}
      </NotificationContext.Provider>
  )
}