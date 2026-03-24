# Build stage
FROM node:20-alpine AS builder

WORKDIR /app/frontend/invest-admin

COPY frontend/invest-admin/package*.json ./
COPY frontend/invest-admin/vite.config.js ./

RUN npm install

COPY frontend/invest-admin/src ./src
COPY frontend/invest-admin/index.html ./
COPY frontend/invest-admin/public ./public
COPY shared/ ../../shared/

RUN chmod -R +x node_modules/.bin 2>/dev/null || true

RUN npm run build


# Runtime stage
FROM nginx:stable

COPY --from=builder /app/frontend/invest-admin/dist /usr/share/nginx/html
COPY frontend/invest-admin/nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
