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
        { path: "blank", name: "blank", meta: { headerTitle: "功能建设中" }, component: () => import("../views/PlaceholderBlank.vue") },
        { path: "orders", name: "orders", meta: { headerTitle: "订单查询" }, component: () => import("../views/OrdersView.vue") },
        {
          path: "order/submit",
          name: "order-submit",
          meta: { headerTitle: "提交订单" },
          component: () => import("../views/OrderSubmitView.vue")
        },
        { path: "order/stats", name: "order-stats", meta: { headerTitle: "订单统计" }, component: () => import("../views/OrderStatsView.vue") },
        { path: "merchants", name: "merchants", meta: { headerTitle: "商家" }, component: () => import("../views/MerchantsView.vue") },
        { path: "agents", name: "agents", meta: { headerTitle: "代理" }, component: () => import("../views/AgentsView.vue") },
        {
          path: "agents/new",
          name: "agent-new",
          meta: { headerTitle: "新增代理" },
          component: () => import("../views/AgentNewView.vue")
        },
        { path: "salesmen", name: "salesmen", meta: { headerTitle: "业务员" }, component: () => import("../views/SalesmenView.vue") },
        {
          path: "salesmen/new",
          name: "salesman-new",
          meta: { headerTitle: "新增业务员" },
          component: () => import("../views/SalesmanNewView.vue")
        },
        { path: "devices", name: "devices", meta: { headerTitle: "设备" }, component: () => import("../views/DevicesView.vue") },
        { path: "accessories", name: "accessories", meta: { headerTitle: "配件管理" }, component: () => import("../views/AccessoriesView.vue") },
        {
          path: "accessories/type/:typeId",
          name: "accessory-type-detail",
          meta: { headerTitle: "配件明细" },
          component: () => import("../views/AccessoryTypeDetailView.vue")
        },
        { path: "work-orders", name: "work-orders", meta: { headerTitle: "工单" }, component: () => import("../views/WorkOrdersView.vue") },
        { path: "stock", name: "stock", meta: { headerTitle: "仓储库存" }, component: () => import("../views/StockView.vue") },
        { path: "ledger", name: "ledger", meta: { headerTitle: "账目流水" }, component: () => import("../views/LedgerView.vue") },
        {
          path: "account/profile",
          name: "account-profile",
          meta: { headerTitle: "账户信息" },
          component: () => import("../views/AccountProfileView.vue")
        },
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
          path: "promo/store/:merchantId",
          name: "promo-store-detail",
          meta: { headerTitle: "店铺详情" },
          component: () => import("../views/promo/PromoStoreDetailView.vue")
        },
        {
          path: "promo/store-new",
          name: "promo-store-new",
          meta: { headerTitle: "新增店铺" },
          component: () => import("../views/promo/PromoStoreNewView.vue")
        },
        {
          path: "promo/merchant-audits",
          name: "promo-merchant-audits",
          meta: { headerTitle: "店铺审核" },
          component: () => import("../views/promo/PromoMerchantAuditsView.vue")
        },
        {
          path: "promo/merchant-audit/:auditId",
          name: "promo-merchant-audit-detail",
          meta: { headerTitle: "审核详情" },
          component: () => import("../views/promo/PromoMerchantAuditDetailView.vue")
        },
        {
          path: "promo/device-new",
          name: "promo-device-new",
          meta: { headerTitle: "新增设备" },
          component: () => import("../views/promo/PromoDeviceNewView.vue")
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
        },
        {
          path: "after/device-mgmt",
          name: "after-device-mgmt",
          meta: { headerTitle: "设备管理" },
          component: () => import("../views/after/AfterDeviceMgmtView.vue")
        },
        {
          path: "after/device-log",
          name: "after-device-log",
          meta: { headerTitle: "设备日志" },
          component: () => import("../views/after/AfterDeviceLogView.vue")
        },
        {
          path: ":pathMatch(.*)*",
          name: "fallback-blank",
          meta: { headerTitle: "功能建设中" },
          component: () => import("../views/PlaceholderBlank.vue")
        }
      ]
    }
  ]
});
