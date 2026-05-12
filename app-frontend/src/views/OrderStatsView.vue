<script setup>
import { computed, inject, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { requestJson } from "../api.js";
import { orderWorkStatusPillClass } from "../utils/statusDisplay.js";

const shell = inject("appShell");
const router = useRouter();
const roleCode = computed(() => shell.portal?.roleCode ?? "");
const canOpenFlow = computed(() => roleCode.value === "main" || roleCode.value === "agent");

function goFlow(orderNo) {
  router.push({ name: "order-flow", params: { orderNo } });
}
const stats = ref(null);
const orders = ref([]);
const err = ref("");
const busy = ref(false);

async function load() {
  err.value = "";
  busy.value = true;
  try {
    const oid = encodeURIComponent(shell.loginOpenid);
    const [statsRes, listRes] = await Promise.all([
      requestJson(`/app-api/order/stats?openid=${oid}`),
      requestJson(`/app-api/order/list?openid=${oid}`)
    ]);
    stats.value = statsRes;
    orders.value = listRes || [];
  } catch (e) {
    err.value = e.message || String(e);
    stats.value = null;
    orders.value = [];
  } finally {
    busy.value = false;
  }
}

watch(() => shell.loginOpenid, load);
onMounted(load);

function orderMidLine(o) {
  const pay = o.payType || "";
  const name = o.merchantName || "";
  if (o.orderTypeCode === "1" || o.orderType === "加油") {
    const b = o.bucketCount != null ? `${o.bucketCount} 桶` : "";
    return `${name} · 加油${b ? " · " + b : ""} · ${pay}`;
  }
  if (o.orderTypeCode === "4" || o.orderType === "转移商家") {
    const to = o.toMerchantName ? ` → ${o.toMerchantName}` : "";
    return `${name}${to} · 转移商家 · ${pay}`;
  }
  return `${name} · ${o.orderType || "—"} · ${pay}`;
}
</script>

<template>
  <div class="page">
    <p v-if="err" class="err">{{ err }}</p>
    <p v-if="busy" class="muted">加载中…</p>

    <div class="card card--stats">
      <h3 class="sub">订单统计</h3>
      <div v-if="stats" class="stats-kpi-grid">
        <div class="stat-kpi">
          <span class="sk-label">角色</span>
          <span class="sk-val">{{ stats.roleName }}</span>
        </div>
        <div class="stat-kpi">
          <span class="sk-label">订单数</span>
          <span class="sk-val">{{ stats.orderCount }}</span>
        </div>
        <div class="stat-kpi">
          <span class="sk-label">金额</span>
          <span class="sk-val">¥{{ stats.amountTotal }}</span>
        </div>
      </div>
      <div v-if="stats?.byStatus?.length" class="stats-title-row">按状态</div>
      <div v-if="stats?.byStatus?.length" class="stat-mini-grid">
        <div v-for="(s, i) in stats.byStatus" :key="`st-${i}`" class="stat-mini stat-mini--border">
          <div class="stat-mini-head">
            <span :class="orderWorkStatusPillClass(s.statusCode)">{{ s.status }}</span>
          </div>
          <div class="stat-mini-body">{{ s.orderCount }} 单</div>
          <div class="stat-mini-amt">¥{{ s.amountTotal }}</div>
        </div>
      </div>
      <div v-if="stats?.byMerchant?.length" class="stats-title-row">按商户</div>
      <div v-if="stats?.byMerchant?.length" class="stat-cards">
        <div v-for="(m, i) in stats.byMerchant" :key="`m-${i}`" class="stat-line-card">
          <span class="stat-line-name">{{ m.merchantName }}</span>
          <span class="stat-line-val">{{ m.orderCount }} 单 · ¥{{ m.amountTotal }}</span>
        </div>
      </div>
      <div v-if="stats?.byAgent?.length" class="stats-title-row">按代理</div>
      <div v-if="stats?.byAgent?.length" class="stat-cards">
        <div v-for="(a, i) in stats.byAgent" :key="`a-${i}`" class="stat-line-card">
          <span class="stat-line-name">代理 #{{ a.agentId }}</span>
          <span class="stat-line-val">{{ a.orderCount }} 单 · ¥{{ a.amountTotal }}</span>
        </div>
      </div>
      <div v-if="!stats" class="muted">暂无统计</div>
    </div>

    <div class="card">
      <h3 class="sub">详细订单</h3>
      <p v-if="canOpenFlow" class="hint-flow">点击卡片查看下单流程时间轴</p>
      <div v-if="!orders.length" class="muted">暂无数据</div>
      <div v-else class="order-list">
        <article
          v-for="(o, i) in orders"
          :key="i"
          class="order-card"
          :class="[
            'order-card--' + (o.statusCode || 'x'),
            { 'order-card--click': canOpenFlow }
          ]"
          @click="canOpenFlow && goFlow(o.orderNo)"
        >
          <div class="order-card__head">
            <span class="no">{{ o.orderNo }}</span>
            <span :class="orderWorkStatusPillClass(o.statusCode)">{{ o.status }}</span>
          </div>
          <div class="order-card__mid">{{ orderMidLine(o) }}</div>
          <div v-if="o.workOrderNo" class="order-card__wo muted">工单 {{ o.workOrderNo }}</div>
          <div class="order-card__bot">
            <span class="amt">¥{{ o.amountPayable }}</span>
            <span class="muted">{{ o.createTime }}</span>
          </div>
        </article>
      </div>
    </div>
  </div>
</template>

<style scoped>
.err {
  color: #b91c1c;
  font-size: 13px;
}
.muted {
  color: #64748b;
  font-size: 12px;
}
.card {
  background: #fff;
  border-radius: 10px;
  padding: 12px;
  margin-bottom: 12px;
  box-shadow: 0 1px 0 rgba(0, 0, 0, 0.04);
}
.sub {
  margin: 0 0 10px;
  font-size: 14px;
}
.card--stats .sub {
  margin-bottom: 12px;
}
.stats-kpi-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-bottom: 12px;
}
.stat-kpi {
  background: #fafbfc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 10px 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.sk-label {
  font-size: 11px;
  color: #64748b;
}
.sk-val {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
  word-break: break-all;
}
.stats-title-row {
  font-size: 12px;
  color: #64748b;
  margin: 12px 0 8px;
}
.stat-mini-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}
.stat-mini {
  border-radius: 10px;
  padding: 10px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-left-width: 4px;
}
.stat-mini--border {
  border-left-color: #cbd5e1;
}
.stat-mini-head {
  margin-bottom: 6px;
}
.stat-mini-body {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}
.stat-mini-amt {
  font-size: 12px;
  color: #64748b;
  margin-top: 4px;
}
.stat-cards {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.stat-line-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  background: #fff;
  font-size: 12px;
}
.stat-line-name {
  color: #334155;
  min-width: 0;
  word-break: break-word;
}
.stat-line-val {
  flex-shrink: 0;
  font-weight: 600;
  color: #0f172a;
}
.hint-flow {
  font-size: 11px;
  color: #64748b;
  margin: -4px 0 10px;
}
.order-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.order-card {
  border-radius: 10px;
  padding: 12px;
  border: 1px solid #e2e8f0;
  border-left-width: 4px;
  background: #fafbfc;
  font-size: 12px;
}
.order-card--0 {
  border-left-color: #f59e0b;
  background: #fffbeb;
}
.order-card--1 {
  border-left-color: #3b82f6;
  background: #eff6ff;
}
.order-card--2 {
  border-left-color: #06b6d4;
  background: #ecfeff;
}
.order-card--3 {
  border-left-color: #059669;
  background: #f0fdf4;
}
.order-card--4 {
  border-left-color: #dc2626;
  background: #fef2f2;
}
.order-card--x {
  border-left-color: #94a3b8;
}
.order-card--click {
  cursor: pointer;
}
.order-card__head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
  font-weight: 600;
}
.no {
  min-width: 0;
  word-break: break-all;
}
.order-card__mid {
  margin-top: 8px;
  color: #334155;
  line-height: 1.45;
}
.order-card__wo {
  margin-top: 6px;
}
.order-card__bot {
  margin-top: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.order-card__bot .amt {
  font-weight: 700;
  color: #0f172a;
}
</style>

