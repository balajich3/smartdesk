import { useState } from 'react'
import { AuthContext } from './authStore.js'
import { clearSession, getSession, loginWithPassword, registerAccount, requestLoginOtp, verifyLoginOtp } from './authService.js'

export function AuthProvider({ children }) {
  const [user, setUser] = useState(getSession)
  async function signIn(email, password) { const authenticated = await loginWithPassword(email, password); setUser(authenticated); return authenticated }
  async function requestOtp(email) { return requestLoginOtp(email) }
  async function signUp(values) { return registerAccount(values) }
  async function verifyOtp(email, otp) { const authenticated = await verifyLoginOtp(email, otp); setUser(authenticated); return authenticated }
  function signOut() { clearSession(); setUser(null) }
  return <AuthContext.Provider value={{ user, signIn, requestOtp, signUp, verifyOtp, signOut }}>{children}</AuthContext.Provider>
}
