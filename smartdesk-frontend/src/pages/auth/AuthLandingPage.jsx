import { Headset } from 'lucide-react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../features/auth/useAuth.js'
import { ROLE_NAVIGATION } from '../../routes/navigation.js'
import AuthFormPage from './AuthFormPage.jsx'

function AuthLandingPage({ initialMode = 'login' }) {
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const { user } = useAuth()
  const mode = pathname === '/signup' ? 'signup' : initialMode

  if (user) return <Navigate replace to={ROLE_NAVIGATION[user.role].homePath} />

  return (
      <main className="grid min-h-svh place-items-center bg-[#f7f7f8] px-4 py-8 sm:px-8">
        <div className="w-full max-w-lg">
          <header className="mb-6 flex items-center gap-2.5">
            <span className="grid size-9 place-items-center rounded-xl bg-neutral-900 text-white"><Headset size={18} /></span>
            <span className="text-base font-semibold tracking-tight text-neutral-900">SmartDesk</span>
          </header>

          <section className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm sm:p-7">
            <div>
              <p className="text-sm font-medium text-neutral-500">Welcome to SmartDesk</p>
              <h1 className="mt-1 text-2xl font-semibold tracking-tight text-neutral-900">Support that works together</h1>
            </div>

            <div aria-label="Choose login or signup" className="mt-5 grid grid-cols-2 rounded-xl bg-neutral-100 p-1" role="tablist">
              {[
                { value: 'login', label: 'Login', path: '/login' },
                { value: 'signup', label: 'Sign Up', path: '/signup' },
              ].map((option) => (
                  <button
                      aria-selected={mode === option.value}
                      className={`rounded-lg px-3 py-2.5 text-sm font-medium transition ${mode === option.value ? 'bg-white text-neutral-900 shadow-sm' : 'text-neutral-500 hover:text-neutral-800'}`}
                      key={option.value}
                      onClick={() => navigate(option.path, { replace: true })}
                      role="tab"
                      type="button"
                  >
                    {option.label}
                  </button>
              ))}
            </div>

            <div className="pt-5">
              <AuthFormPage mode={mode} />
            </div>
          </section>
        </div>
      </main>
  )
}

export default AuthLandingPage
