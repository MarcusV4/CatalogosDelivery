import { createBrowserRouter } from 'react-router-dom';
import { EstadoMensagem } from './components/EstadoMensagem';
import { Layout } from './components/Layout';
import { RotaProtegida } from './components/RotaProtegida';
import { Home } from './pages/Home';
import { Login, RetornoOAuth } from './pages/Login';
import { NaoEncontrada } from './pages/NaoEncontrada';
import { PainelCardapio } from './pages/PainelCardapio';
import { PainelRestaurantes } from './pages/PainelRestaurantes';
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
      { path: 'oauth/callback', element: <RetornoOAuth /> },
      { path: 'restaurante/:id', element: <PaginaRestaurante /> },
      {
        path: 'painel',
        element: <RotaProtegida />,
        children: [
          { path: 'restaurantes', element: <PainelRestaurantes /> },
          {
            path: 'restaurantes/:restauranteId/cardapio',
            element: <PainelCardapio />,
          },
          { path: '*', element: <NaoEncontrada /> },
        ],
      },
      { path: '*', element: <NaoEncontrada /> },
    ],
  },
]);
