<script setup>
import { computed, inject, onMounted, ref, watch } from "vue";
import { requestJson } from "../api.js";

const shell = inject("appShell");
const roleCode = computed(() => shell.portal?.roleCode ?? "");
const rows = ref([]);
const stats = ref(null);
const err = ref("");
const busy = ref(false);

async function load() {
  err.value = "";
  busy.value = true;
  try {
    const oid = shell.loginOpenid;
    const q = encodeURIComponent(oid);
    const [listRes, statRes] = await Promise.all([
      requestJson(`/app-api/order/list?openid=${q}`),
      requestJson(`/app-api/order/stats?openid=${q}`)
    ]);
    rows.value = listRes;
    stats.value = statRes;
  } catch (e) {
    err.value = e.message || String(e);
    rows.value = [];
    stats.value = null;
  } finally {
    busy.value = false;
  }
}

async function onConfirm(orderNo) {
  if (!window.confirm(`确认订单 ${orderNo}？将生成待分配工单。`)) {
    return;
  }
  err.value = "";
  try {
    const oid = shell.loginOpenid;
    await requestJson(`/app-api/order/confirm/${encodeURIComponent(orderNo)}?openid=${encodeURIComponent(oid)}`, {
      method: "POST"
    });
    await load();
  } catch (e) {
    err.value = e.message || String(e);
  }
}

async function onCancel(orderNo) {
  if (!window.confirm(`取消订单 ${orderNo}？`)) {
    return;
  }
  err.value = "";
  try {
    const oid = shell.loginOpenid;
    await requestJson(`/app-api/order/cancel/${encodeURIComponent(orderNo)}?openid=${encodeURIComponent(oid)}`, {
      method: "POST"
    });
    await load();
  } catch (e) {
    err.value = e.message || String(e);
  }
}

function orderMidLine(o) {
  const pay = o.payType || "";
  const name = o.merchantName || "";
  if (o.orderTypeCode === "1" || o.orderType === "加油") {
    const b = o.bucketCount != null ? `${o.bucketCount} 桶` : "";
    return `${name} · 加油${b ? " · " + b : ""} · ${pay}`;
  }
  return `${name} · ${o.orderType || "—"} · ${pay}`;
}

function canCancelRow(o) {
  if (o.statusCode === "3" || o.statusCode === "4") {
    return false;
  }
  if (roleCode.value === "merchant") {
    return o.statusCode === "0" || o.statusCode === "1";
  }
  if (roleCode.value === "main" || roleCode.value === "agent") {
    return o.statusCode === "0" || o.statusCode === "1" || o.statusCode === "2";
  }
  return false;
}

watch(() => shell.loginOpenid, () => {
  load();
});

onMounted(() => {
  load();
});
</script>

<template>
  <div class="page">
    <h2 class="page-title">订单查询</h2>
    <p v-if="err" class="err">{{ err }}</p>
    <p v-if="busy" class="muted">加载中…</p>

    <div class="card">
      <h3 class="sub">订单统计</h3>
      <div v-if="stats" class="stats-top">
        <div class="kpi">角色：{{ stats.roleName }}</div>
        <div class="kpi">订单数：{{ stats.orderCount }}</div>
        <div class="kpi">金额：¥{{ stats.amountTotal }}</div>
      </div>
      <div v-if="stats?.byStatus?.length" class="stats-block">
        <div class="stats-title">按状态</div>
        <div v-for="(s, i) in stats.byStatus" :key="`st-${i}`" class="stats-row">
          <span>{{ s.status }}</span>
          <span>{{ s.orderCount }} 单 · ¥{{ s.amountTotal }}</span>
        </div>
      </div>
      <div v-if="stats?.byMerchant?.length" class="stats-block">
        <div class="stats-title">按商户</div>
        <div v-for="(m, i) in stats.byMerchant" :key="`m-${i}`" class="stats-row">
          <span>{{ m.merchantName }}</span>
          <span>{{ m.orderCount }} 单 · ¥{{ m.amountTotal }}</span>
        </div>
      </div>
      <div v-if="stats?.byAgent?.length" class="stats-block">
        <div class="stats-title">按代理</div>
        <div v-for="(a, i) in stats.byAgent" :key="`a-${i}`" class="stats-row">
          <span>代理 #{{ a.agentId }}</span>
          <span>{{ a.orderCount }} 单 · ¥{{ a.amountTotal }}</span>
        </div>
      </div>
    </div>

    <div class="card">
      <h3 class="sub">订单列表</h3>
      <div v-if="!rows.length" class="muted">暂无数据</div>
      <div v-for="(o, i) in rows" :key="i" class="item">
        <div class="item-top">
          <span class="no">{{ o.orderNo }}</span>
          <span class="st">{{ o.status }}</span>
        </div>
        <div class="item-mid">{{ orderMidLine(o) }}</div>
        <div v-if="o.workOrderNo" class="item-mid muted">工单 {{ o.workOrderNo }}</div>
        <div class="item-bot">
          <span>¥{{ o.amountPayable }}</span>
          <span class="muted">{{ o.createTime }}</span>
        </div>
        <div class="item-actions">
          <button
            v-if="(roleCode === 'main' || roleCode === 'agent') && o.statusCode === '0'"
            type="button"
            class="link"
            @click="onConfirm(o.orderNo)"
          >
            确认
          </button>
          <button v-if="canCancelRow(o)" type="button" class="link danger" @click="onCancel(o.orderNo)">取消</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-title {
  margin: 0 0 10px;
  font-size: 16px;
  font-weight: 600;
}
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
.item {
  border-top: 1px solid #eef1f6;
  padding: 10px 0;
  font-size: 12px;
}
.item:first-of-type {
  border-top: none;
  padding-top: 0;
}
.item-top {
  display: flex;
  justify-content: space-between;
  font-weight: 600;
}
.item-mid,
.item-bot {
  margin-top: 4px;
  color: #334155;
}
.item-bot {
  display: flex;
  justify-content: space-between;
}
.item-actions {
  margin-top: 6px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.stats-top {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}
.kpi {
  background: #f5f8ff;
  border: 1px solid #dbe7ff;
  border-radius: 8px;
  padding: 8px;
  font-size: 12px;
  color: #1e3a8a;
}
.stats-block {
  margin-top: 10px;
}
.stats-title {
  font-size: 12px;
  color: #64748b;
  margin-bottom: 6px;
}
.stats-row {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  border-top: 1px dashed #eef1f6;
  font-size: 12px;
}
.link {
  border: none;
  background: none;
  color: #1f6dff;
  cursor: pointer;
  padding: 0;
  font-size: 12px;
}
.link.danger {
  color: #b91c1c;
}
</style>
