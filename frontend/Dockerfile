# syntax=docker/dockerfile:1
# Build estático do front servido pelo nginx. As VITE_* são gravadas no JavaScript durante o
# build: o navegador chama as APIs direto, então use endereços que o NAVEGADOR alcança
# (ex.: http://localhost:8004), e não os nomes internos dos serviços do Docker.
FROM node:24-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci

COPY . .
ARG VITE_API_RESTAURANTES_URL=http://localhost:8004
ARG VITE_API_CARDAPIO_URL=http://localhost:8005
ARG VITE_API_BUSCA_URL=http://localhost:8006
ARG VITE_API_IDENTIDADE_URL=http://127.0.0.1:18001
ARG VITE_IDENTIDADE_CLIENT_ID=
ARG VITE_IDENTIDADE_REDIRECT_URI=http://localhost:3000/oauth/callback
ARG VITE_IDENTIDADE_SCOPES=
RUN npm run build

FROM nginx:1.30-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
