FROM node:20-alpine

WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY anti-macro-service/package*.json ./
RUN npm ci --omit=dev

COPY anti-macro-service/dist/ ./dist/

ENV PORT=8080

USER appuser

EXPOSE 8080

ENTRYPOINT ["node", "dist/index.js"]
