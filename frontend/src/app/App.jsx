import AppRouter from './router.jsx';
import Providers from './providers.jsx';
import GlobalStatusModal from '@shared/ui/GlobalStatusModal.jsx';
import GlobalConfirmModal from '@shared/ui/GlobalConfirmModal.jsx';
import { ErrorBoundary } from '@shared/ui';

function App() {
  return (
    <ErrorBoundary fullScreen>
      <Providers>
        <AppRouter />
        <GlobalStatusModal />
        <GlobalConfirmModal />
      </Providers>
    </ErrorBoundary>
  );
}

export default App;
