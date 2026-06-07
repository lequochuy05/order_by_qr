import { BrowserRouter } from 'react-router-dom'
import { Toaster } from 'react-hot-toast'
import { AuthProvider } from '@modules/auth/model/AuthContext.jsx'

const Providers = ({ children }) => (
  <AuthProvider>
    <Toaster position="top-right" reverseOrder={false} />
    <BrowserRouter>
      {children}
    </BrowserRouter>
  </AuthProvider>
)

export default Providers
