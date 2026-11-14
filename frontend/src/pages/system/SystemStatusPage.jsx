import { useParams } from 'react-router-dom';
import { SystemErrorPage } from '@shared/ui';

const SystemStatusPage = ({ statusCode }) => {
  const params = useParams();

  return <SystemErrorPage statusCode={statusCode || params.statusCode} />;
};

export default SystemStatusPage;
