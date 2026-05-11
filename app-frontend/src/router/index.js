import { createRouter, createWebHashHistory } from "vue-router";
import Layout from "../views/Layout.vue";

export const router = createRouter({
  history: createWebHashHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: "/",
      component: Layout,
      children: [
        { path: "", name: "home", component: () => import("../views/Home.vue") },
        { path: "orders", name: "orders", component: () => import("../views/OrdersView.vue") },
        { path: "merchants", name: "merchants", component: () => import("../views/MerchantsView.vue") },
        { path: "agents", name: "agents", component: () => import("../views/AgentsView.vue") },
        { path: "salesmen", name: "salesmen", component: () => import("../views/SalesmenView.vue") },
        { path: "devices", name: "devices", component: () => import("../views/DevicesView.vue") }
      ]
    }
  ]
});
