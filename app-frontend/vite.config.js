import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
  base: "/app/",
  plugins: [vue()],
  build: {
    outDir: "dist-app"
  },
  server: {
    port: 8082,
    proxy: {
      "/app-api": {
        target: "http://127.0.0.1:7267",
        changeOrigin: true
      }
    }
  }
});
