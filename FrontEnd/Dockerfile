# --- Stage 1: build the static assets ---
FROM node:20-alpine AS build
WORKDIR /app

# Vite only reads VITE_* variables at build time (npm run build), baking
# them into the compiled JS — a runtime `environment:` entry in
# docker-compose.yml would be too late to affect the already-built bundle.
# These must come in as build args instead.
ARG VITE_API_URL
ENV VITE_API_URL=$VITE_API_URL

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

# --- Stage 2: serve with nginx ---
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
