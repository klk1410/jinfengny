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
        { path: "devices", name: "devices", component: () => import("../views/DevicesView.vue") },
        { path: "work-orders", name: "work-orders", component: () => import("../views/WorkOrdersView.vue") },
        { path: "stock", name: "stock", component: () => import("../views/StockView.vue") },
        { path: "ledger", name: "ledger", component: () => import("../views/LedgerView.vue") },
        {
          path: "promo/coops",
          name: "promo-coops",
          meta: { headerTitle: "合作管理" },
          component: () => import("../views/promo/PromoCoopsView.vue")
        },
        {
          path: "promo/coop-new",
          name: "promo-coop-new",
          meta: { headerTitle: "新增合作" },
          component: () => import("../views/promo/PromoCoopNewView.vue")
        },
        {
          path: "promo/stores",
          name: "promo-stores",
          meta: { headerTitle: "店铺管理" },
          component: () => import("../views/promo/PromoStoresView.vue")
        },
        {
          path: "promo/store-new",
          name: "promo-store-new",
          meta: { headerTitle: "新增店铺" },
          component: () => import("../views/promo/PromoStoreNewView.vue")
        },
        {
          path: "promo/devices",
          name: "promo-devices",
          meta: { headerTitle: "推广设备" },
          component: () => import("../views/promo/PromoDevicesView.vue")
        },
        {
          path: "promo/withdraws",
          name: "promo-withdraws",
          meta: { headerTitle: "提现管理" },
          component: () => import("../views/promo/PromoWithdrawsView.vue")
        },
        {
          path: "promo/prepaids",
          name: "promo-prepaids",
          meta: { headerTitle: "预付款管理" },
          component: () => import("../views/promo/PromoPrepaidsView.vue")
        }
      ]
    }
  ]
});
