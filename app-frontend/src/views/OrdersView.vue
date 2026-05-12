<script setup>
import { computed, inject, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { requestJson } from "../api.js";
import { orderWorkStatusPillClass } from "../utils/statusDisplay.js";

const shell = inject("appShell");
const router = useRouter();
const roleCode = computed(() => shell.portal?.roleCode ?? "");
const canOpenFlow = computed(() => roleCode.value === "main" || roleCode.value === "agent");
const rows = ref([]);
const err = ref("");
const busy = ref(false);

async function load() {
  err.value = "";
  busy.value = true;
  try {
    const oid = shell.loginOpenid;
    const q = encodeURIComponent(oid);
    rows.value = await requestJson(`/app-api/order/list?openid=${q}`);
  } catch (e) {
    err.value = e.message || String(e);
    rows.value = [];
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
  if (o.orderTypeCode === "4" || o.orderType === "转移商家") {
    const to = o.toMerchantName ? ` → ${o.toMerchantName}` : "";
    return `${name}${to} · 转移商家 · ${pay}`;
  }
  return `${name} · ${o.orderType || "—"} · ${pay}`;
}

function goFlow(orderNo) {
  router.push({ name: "order-flow", params: { orderNo } });
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
      <h3 class="sub">订单列表</h3>
      <p v-if="canOpenFlow" class="hint-flow">点击卡片查看下单流程时间轴</p>
      <div v-if="!rows.length" class="muted">暂无数据</div>
      <div v-else class="order-list">
        <article
          v-for="(o, i) in rows"
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
          <div class="order-card__actions" @click.stop>
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
        </article>
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
.order-card__actions {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
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
