import { QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { BrowserRouter } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { AuthProvider } from '@features/auth';
import { queryClient } from '@shared/api/queryClient.js';
import { useRegisterSW } from '@shared/lib/pwa.js';
import { WebSocketInvalidator } from './WebSocketInvalidator.jsx';

const Providers = ({ children }) => {
  useRegisterSW();

  return (
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
};

export default Providers;
