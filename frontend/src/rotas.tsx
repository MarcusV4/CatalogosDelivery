import { createBrowserRouter } from 'react-router-dom';
import { EstadoMensagem } from './components/EstadoMensagem';
import { Layout } from './components/Layout';
import { Home } from './pages/Home';
import { Login } from './pages/Login';
import { NaoEncontrada } from './pages/NaoEncontrada';
import { PaginaRestaurante } from './pages/Restaurante';

export const roteador = createBrowserRouter([
  {
    element: <Layout />,
    errorElement: (
      <Layout>
        <EstadoMensagem
          tipo="erro"
          titulo="Algo deu errado."
          descricao="Recarregue a página. Se o problema continuar, avise a equipe."
        />
      </Layout>
    ),
    children: [
      { index: true, element: <Home /> },
      { path: 'login', element: <Login /> },
      { path: 'restaurante/:id', element: <PaginaRestaurante /> },
      { path: '*', element: <NaoEncontrada /> },
    ],
  },
]);
