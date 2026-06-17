import { QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { BrowserRouter } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { AuthProvider } from '@modules/auth/model/AuthContext.jsx';
import { queryClient } from '@shared/api/queryClient.js';
import { WebSocketInvalidator } from './WebSocketInvalidator.jsx';

const Providers = ({ children }) => (
  <QueryClientProvider client={queryClient}>
    <AuthProvider>
      <Toaster position="top-right" reverseOrder={false} />
      <BrowserRouter>
        <WebSocketInvalidator />
        {children}
      </BrowserRouter>
    </AuthProvider>
    {import.meta.env.DEV && <ReactQueryDevtools initialIsOpen={false} />}
  </QueryClientProvider>
);

export default Providers;
