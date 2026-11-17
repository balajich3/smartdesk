# SmartDesk Frontend

React + Vite frontend integrated against the SmartDesk backend source supplied with this project.

## Run locally

1. Start PostgreSQL and Kafka as required by the backend.
2. Start the backend services, including Eureka, API Gateway, Ticket Service, and Notification Service.
3. From this folder run:

```bash
npm install
npm run dev
```

Open the Vite URL (normally `http://localhost:5173`).

## API routing

The Vite development proxy forwards:
- `/ticket-service/**` to `http://localhost:8080` (API Gateway discovery route)
- `/notification-service/**` to `http://localhost:8080` (API Gateway discovery route)

The browser WebSocket connects directly to Notification Service:
- `ws://localhost:8083/ws-notifications`

Override the WebSocket URL with `VITE_NOTIFICATION_WS_URL` if needed. Production deployments must provide equivalent reverse-proxy routes or set `VITE_TICKET_API_BASE_URL` and `VITE_NOTIFICATION_API_BASE_URL` to reachable API base paths.

## Implemented backend integration

- Authentication: register, login, OTP verification, JWT persistence, logout.
- Roles: EMPLOYEE, SUPPORT_AGENT, MANAGER, ADMIN.
- Departments: IT, HR, FACILITIES (the department codes accepted by Ticket Service).
- Tickets: employee/agent create, own tickets, agent queue, assigned tickets, manager department tickets, claim, status update, comments, resolution acknowledgement.
- Notifications: notification history, mark read, mark all read (implemented as individual mark-read calls), unread count via returned list, WebSocket user notifications.
- Manager: dashboard statistics and pending support-agent approvals.
- Admin: user listing and active/inactive account updates.

## Backend contract constraints reflected in the UI

- Registration is available only for EMPLOYEE and SUPPORT_AGENT. Manager/Admin accounts are provisioned/managed through backend administration.
- Registration requires a department code for both roles.
- The backend claim endpoint currently changes OPEN directly to IN_PROGRESS; it does not use ASSIGNED as the resulting claim status.
- Resolution acknowledgement is currently restricted to EMPLOYEE by the backend.
- The manager ticket endpoint returns the manager's department tickets; it is not an organization-wide admin ticket endpoint.
- The current backend has no ticket-history endpoint or individual ticket GET endpoint. Ticket detail pages are populated from the role's ticket list; the visible activity combines ticket creation and accessible comments.
- Ticket claim/status notification events are not currently emitted from the backend claim/status methods. Ticket creation notifications and resolution-acknowledgement notifications are wired in the supplied source. Frontend WebSocket support is ready for notifications the backend publishes.

## Notes

The frontend uses the existing visual components and pages; demo authentication, demo ticket persistence, and seeded demo notifications are no longer used by the active providers.
