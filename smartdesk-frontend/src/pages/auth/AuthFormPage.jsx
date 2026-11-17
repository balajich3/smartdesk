import { useState } from 'react'
import { LockKeyhole, Mail, UserRound } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { DEPARTMENTS } from '../../features/auth/authService.js'
import { useAuth } from '../../features/auth/useAuth.js'
import { ROLES } from '../../context/roles.js'

const SIGNUP_ROLES = [
  { value: ROLES.EMPLOYEE, label: 'Employee' },
  { value: ROLES.AGENT, label: 'Support Agent' },
  { value: ROLES.MANAGER, label: 'Manager' },
]

function AuthFormPage({ mode }) {
  const navigate = useNavigate()
  const { signIn, requestOtp, signUp } = useAuth()
  const [role, setRole] = useState(ROLES.EMPLOYEE)
  const [departmentCode, setDepartmentCode] = useState('IT')
  const [loginMethod, setLoginMethod] = useState('password')
  const [form, setForm] = useState({ name: '', email: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const isSignup = mode === 'signup'
  const needsDepartment = role === ROLES.AGENT || role === ROLES.MANAGER

  function updateField(event) { setError(''); setForm(current => ({ ...current, [event.target.name]: event.target.value })) }

  async function handleSubmit(event) {
    event.preventDefault(); setError(''); setLoading(true)
    try {
      if (isSignup) {
        await signUp({ ...form, role, departmentCode: needsDepartment ? departmentCode : undefined })
        const message = role === ROLES.EMPLOYEE
            ? 'Registration successful. You can log in now.'
            : 'Registration submitted. An administrator must approve your account before login.'
        toast.success(message); navigate('/login', { replace: true })
      } else if (loginMethod === 'password') {
        await signIn(form.email, form.password)
        toast.success('Login successful.')
        // AuthContext stores the session; route according to the authenticated role.
        const session = JSON.parse(localStorage.getItem('smartdesk-user') || 'null')
        const paths = { EMPLOYEE: '/employee', SUPPORT_AGENT: '/agent', MANAGER: '/manager', ADMIN: '/admin' }
        navigate(paths[session?.role] || '/', { replace: true })
      } else {
        await requestOtp(form.email)
        toast.success('OTP sent to your registered email.')
        navigate('/verify-otp')
      }
    } catch (authError) {
      const message = authError.response?.data?.message || authError.response?.data || authError.message || 'Request failed'
      const readable = typeof message === 'string' ? message : 'Please check your details and try again.'
      setError(readable); toast.error(readable)
    } finally { setLoading(false) }
  }

  return <div>
    <h2 className="text-xl font-semibold tracking-tight text-neutral-900">{isSignup ? 'Create your account' : 'Welcome back'}</h2>
    <p className="mt-2 text-sm leading-5 text-neutral-500">{isSignup ? 'Choose your workspace and enter your details to get started.' : 'Sign in to your SmartDesk workspace.'}</p>
    <form className="mt-5 space-y-4" onSubmit={handleSubmit}>
      {isSignup && <>
        <label className="block space-y-1.5"><span className="text-xs font-medium text-neutral-700">I am signing up as</span>
          <select className="h-11 w-full rounded-lg border border-neutral-200 bg-white px-3 text-sm" value={role} onChange={e => setRole(e.target.value)}>
            {SIGNUP_ROLES.map(option => <option key={option.value} value={option.value}>{option.label}</option>)}
          </select>
        </label>
        {needsDepartment && <label className="block space-y-1.5"><span className="text-xs font-medium text-neutral-700">Department</span>
          <select className="h-11 w-full rounded-lg border border-neutral-200 bg-white px-3 text-sm" value={departmentCode} onChange={e => setDepartmentCode(e.target.value)}>
            {DEPARTMENTS.map(department => <option key={department.code} value={department.code}>{department.label}</option>)}
          </select>
        </label>}
        <label className="block space-y-1.5"><span className="text-xs font-medium text-neutral-700">Full name</span><span className="relative block"><UserRound className="absolute left-3 top-1/2 -translate-y-1/2 text-neutral-400" size={16}/><input autoComplete="name" className="h-11 w-full rounded-lg border border-neutral-200 pl-10 pr-3 text-sm" maxLength={100} name="name" onChange={updateField} placeholder="Your name" required value={form.name}/></span></label>
      </>}
      <label className="block space-y-1.5"><span className="text-xs font-medium text-neutral-700">Email address</span><span className="relative block"><Mail className="absolute left-3 top-1/2 -translate-y-1/2 text-neutral-400" size={16}/><input autoComplete="email" className="h-11 w-full rounded-lg border border-neutral-200 pl-10 pr-3 text-sm" name="email" onChange={updateField} placeholder="you@example.com" required type="email" value={form.email}/></span></label>
      {!isSignup && <div className="grid grid-cols-2 rounded-lg bg-neutral-100 p-1 text-xs"><button type="button" onClick={() => setLoginMethod('password')} className={`rounded-md py-2 ${loginMethod === 'password' ? 'bg-white shadow-sm font-semibold' : 'text-neutral-500'}`}>Password</button><button type="button" onClick={() => setLoginMethod('otp')} className={`rounded-md py-2 ${loginMethod === 'otp' ? 'bg-white shadow-sm font-semibold' : 'text-neutral-500'}`}>Email OTP</button></div>}
      {(!isSignup && loginMethod === 'otp') ? <p className="rounded-lg bg-blue-50 px-3 py-3 text-xs text-blue-800">We'll send a one-time code to your registered email address.</p> : <label className="block space-y-1.5"><span className="text-xs font-medium text-neutral-700">Password</span><span className="relative block"><LockKeyhole className="absolute left-3 top-1/2 -translate-y-1/2 text-neutral-400" size={16}/><input autoComplete={isSignup ? 'new-password' : 'current-password'} className="h-11 w-full rounded-lg border border-neutral-200 pl-10 pr-3 text-sm" minLength={isSignup ? 8 : undefined} maxLength={72} name="password" onChange={updateField} placeholder={isSignup ? 'At least 8 characters' : 'Your password'} required={isSignup || loginMethod === 'password'} type="password" value={form.password}/></span></label>}
      {error && <p aria-live="polite" className="rounded-lg bg-red-50 px-3 py-2 text-xs text-red-700">{error}</p>}
      <button disabled={loading} className="inline-flex h-11 w-full items-center justify-center rounded-lg bg-neutral-900 text-sm font-medium text-white transition hover:bg-neutral-700 disabled:opacity-50" type="submit">{loading ? 'Please wait…' : isSignup ? 'Create account' : loginMethod === 'otp' ? 'Send OTP' : 'Login'}</button>
    </form>
  </div>
}
export default AuthFormPage
