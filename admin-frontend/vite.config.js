import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 8081,
    proxy: {
      "/prod-api": {
        target: "http://127.0.0.1:7266",
        changeOrigin: true
      }
    }
  }
});
