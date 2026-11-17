import api from '../../services/api.js'
import { ROLES } from '../../context/roles.js'

const USER_KEY = 'smartdesk-user'
const TOKEN_KEY = 'accessToken'
const PENDING_EMAIL_KEY = 'smartdesk-pending-email'

export const DEPARTMENTS = [
  { code: 'IT', label: 'Information Technology' },
  { code: 'HR', label: 'Human Resources' },
  { code: 'FACILITIES', label: 'Facilities' },
]

export function mapAuthUser(data) {
  const role = data.roles?.[0] || ROLES.EMPLOYEE
  return { id: data.userId, name: data.fullName, email: data.email, role, department: data.department || null }
}

export async function registerAccount(values) {
  const payload = {
    role: values.role,
    fullName: values.name.trim(),
    email: values.email.trim(),
    password: values.password,
    ...(values.role !== ROLES.EMPLOYEE ? { departmentCode: values.departmentCode } : {}),
  }
  const { data } = await api.post('/api/auth/register', payload)
  return data
}

export async function loginWithPassword(email, password) {
  const { data } = await api.post('/api/auth/login/password', { email, password })
  return saveAuthSession(data)
}

export async function requestLoginOtp(email) {
  const { data } = await api.post('/api/auth/login/otp', { email })
  localStorage.setItem(PENDING_EMAIL_KEY, email.trim())
  return data
}

export async function verifyLoginOtp(email, otp) {
  const { data } = await api.post('/api/auth/verify-otp', { email, otp })
  localStorage.removeItem(PENDING_EMAIL_KEY)
  return saveAuthSession(data)
}

function saveAuthSession(authData) {
  const user = mapAuthUser(authData)
  localStorage.setItem(TOKEN_KEY, authData.accessToken)
  localStorage.setItem(USER_KEY, JSON.stringify(user))
  return user
}

export function getPendingEmail() { return localStorage.getItem(PENDING_EMAIL_KEY) }
export function getSession() {
  try {
    const token = localStorage.getItem(TOKEN_KEY)
    const user = JSON.parse(localStorage.getItem(USER_KEY) || 'null')
    return token && user && Object.values(ROLES).includes(user.role) ? user : null
  } catch { return null }
}
export function clearSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  localStorage.removeItem(PENDING_EMAIL_KEY)
}
