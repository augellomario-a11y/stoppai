FROM node:20-alpine
RUN apk add --no-cache sox
WORKDIR /app
COPY package*.json ./
RUN npm ci --omit=dev
COPY . .
EXPOSE 6002
CMD ["node", "server.js"]
