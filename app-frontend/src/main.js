import { createApp } from "vue";
import App from "./App.vue";
import { router } from "./router";
import "./styles/statusBadges.css";
import "./styles/detailCards.css";

createApp(App).use(router).mount("#app");
