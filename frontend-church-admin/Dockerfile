# 構建階段
FROM node:20-alpine AS builder

WORKDIR /app

# 複製 package 文件
COPY frontend-church-admin/package*.json ./
COPY frontend-church-admin/vite.config.js ./

# 安裝依賴
RUN npm install

# 複製源代碼（排除 node_modules）
COPY frontend-church-admin/src ./src
COPY frontend-church-admin/index.html ./
COPY frontend-church-admin/public ./public
COPY shared/ ./shared/

# 確保 node_modules/.bin 有執行權限
RUN chmod -R +x node_modules/.bin 2>/dev/null || true

# 構建應用
RUN npm run build


# 生產階段
FROM nginx:stable
# FROM nginx:alpine

# 安裝 wget 用於健康檢查
# RUN apk add --no-cache wget

# 複製構建結果到 nginx
COPY --from=builder /app/dist /usr/share/nginx/html

# 複製 nginx 配置文件（支持 Vue Router history 模式）
COPY frontend-church-admin/nginx.conf /etc/nginx/conf.d/default.conf

# 暴露端口
EXPOSE 80

# nginx 會自動啟動

