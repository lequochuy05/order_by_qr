import AppRouter from './router.jsx';
import Providers from './providers.jsx';
import GlobalStatusModal from '@shared/ui/GlobalStatusModal.jsx';
import GlobalConfirmModal from '@shared/ui/GlobalConfirmModal.jsx';

function App() {
  return (
    <Providers>
      <AppRouter />
      <GlobalStatusModal />
      <GlobalConfirmModal />
    </Providers>
  );
}

export default App;
