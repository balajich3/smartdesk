import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { useAuth } from '../../features/auth/useAuth.js'
import { getPendingEmail } from '../../features/auth/authService.js'
import { ROLE_NAVIGATION } from '../../routes/navigation.js'

export default function VerifyOtpPage() {
  const navigate = useNavigate()
  const { verifyOtp } = useAuth()
  const email = getPendingEmail()
  const [otp, setOtp] = useState('')
  const [loading, setLoading] = useState(false)

  if (!email) return <Navigate to="/login" replace />

  async function handleSubmit(event) {
    event.preventDefault()
    setLoading(true)
    try {
      const user = await verifyOtp(email, otp)
      toast.success('Login successful')
      navigate(ROLE_NAVIGATION[user.role]?.homePath || '/login', { replace: true })
    } catch (error) {
      const message = error.response?.data?.message || error.response?.data || 'OTP verification failed'
      toast.error(typeof message === 'string' ? message : 'OTP verification failed')
    } finally {
      setLoading(false)
    }
  }

  return (
      <main className="grid min-h-svh place-items-center bg-[#f7f7f8] px-4">
        <form onSubmit={handleSubmit} className="w-full max-w-md rounded-2xl border border-neutral-200 bg-white p-7 shadow-sm">
          <p className="text-sm text-neutral-500">SmartDesk account security</p>
          <h1 className="mt-2 text-2xl font-semibold text-neutral-900">Verify your email</h1>
          <p className="mt-2 text-sm text-neutral-500">Enter the six-digit OTP sent to {email}.</p>
          <input autoFocus inputMode="numeric" pattern="[0-9]{6}" maxLength={6} required value={otp} onChange={(event) => setOtp(event.target.value.replace(/\D/g, ''))} placeholder="000000" className="mt-6 h-14 w-full rounded-lg border border-neutral-200 text-center text-2xl tracking-[0.5em] outline-none focus:border-neutral-500" />
          <button disabled={loading || otp.length !== 6} className="mt-4 h-11 w-full rounded-lg bg-neutral-900 text-sm font-medium text-white disabled:opacity-50">{loading ? 'Verifying…' : 'Verify and continue'}</button>
          <button type="button" onClick={() => navigate('/login')} className="mt-4 w-full text-sm text-neutral-500 hover:text-neutral-900">Back to login</button>
        </form>
      </main>
  )
}
