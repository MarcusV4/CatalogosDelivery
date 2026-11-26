import { RouterProvider } from 'react-router-dom';
import { NotificacaoProvider } from './context/NotificacaoProvider';
import { AutenticacaoProvider } from './context/AutenticacaoProvider';
import { ProvedorConsultas } from './context/ProvedorConsultas';
import { roteador } from './rotas';

export default function App() {
  return (
    <NotificacaoProvider>
      <AutenticacaoProvider>
        <ProvedorConsultas>
          <RouterProvider router={roteador} />
        </ProvedorConsultas>
      </AutenticacaoProvider>
    </NotificacaoProvider>
  );
}
