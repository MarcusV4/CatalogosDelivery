import { RouterProvider } from 'react-router-dom';
import { NotificacaoProvider } from './context/NotificacaoProvider';
import { ProvedorConsultas } from './context/ProvedorConsultas';
import { roteador } from './rotas';

export default function App() {
  return (
    <NotificacaoProvider>
      <ProvedorConsultas>
        <RouterProvider router={roteador} />
      </ProvedorConsultas>
    </NotificacaoProvider>
  );
}
