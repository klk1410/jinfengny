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

/** 代理确认订单：预设时长（小时），与后端下拉取值一致 */
const ESTIMATED_HOUR_OPTIONS = [
  { value: 0.5, label: "0.5 小时" },
  { value: 1, label: "1 小时" },
  { value: 1.5, label: "1.5 小时" },
  { value: 2, label: "2 小时" },
  { value: 2.5, label: "2.5 小时" },
  { value: 3, label: "3 小时" },
  { value: 4, label: "4 小时" },
  { value: 5, label: "5 小时" },
  { value: 6, label: "6 小时" },
  { value: 8, label: "8 小时" },
  { value: 12, label: "12 小时" },
  { value: 24, label: "24 小时" },
  { value: 48, label: "48 小时" },
  { value: 72, label: "72 小时" }
];

const confirmModal = ref(false);
const confirmTargetNo = ref("");
const estimatedHours = ref(2);
const confirmBusy = ref(false);

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
  if (roleCode.value === "agent") {
    confirmTargetNo.value = orderNo;
    estimatedHours.value = 2;
    confirmModal.value = true;
    return;
  }
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

function closeConfirmModal() {
  if (confirmBusy.value) {
    return;
  }
  confirmModal.value = false;
}

async function submitAgentConfirm() {
  const orderNo = confirmTargetNo.value;
  err.value = "";
  confirmBusy.value = true;
  try {
    const oid = shell.loginOpenid;
    const h = Number(estimatedHours.value);
    await requestJson(
      `/app-api/order/confirm/${encodeURIComponent(orderNo)}?openid=${encodeURIComponent(oid)}&estimatedWorkHours=${encodeURIComponent(h)}`,
      { method: "POST" }
    );
    confirmModal.value = false;
    await load();
  } catch (e) {
    err.value = e.message || String(e);
  } finally {
    confirmBusy.value = false;
  }
}

function formatEstimatedHours(v) {
  if (v == null || v === "") {
    return "";
  }
  const n = Number(v);
  if (!Number.isFinite(n)) {
    return String(v);
  }
  return Number.isInteger(n) ? String(n) : String(n);
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
          <div v-if="o.estimatedWorkHours != null && o.statusCode !== '0'" class="order-card__hours muted">
            预计工时 {{ formatEstimatedHours(o.estimatedWorkHours) }} 小时
          </div>
          <div class="order-card__bot">
            <span class="amt">¥{{ o.amountPayable }}</span>
            <span class="muted">{{ o.createTime }}</span>
          </div>
          <div class="order-card__actions" @click.stop>
            <button
              v-if="(roleCode === 'main' || roleCode === 'agent') && o.statusCode === '0'"
              type="button"
              class="btn-act btn-act--primary"
              @click="onConfirm(o.orderNo)"
            >
              确认
            </button>
            <button
              v-if="canCancelRow(o)"
              type="button"
              class="btn-act btn-act--danger"
              @click="onCancel(o.orderNo)"
            >
              取消
            </button>
          </div>
        </article>
      </div>
    </div>

    <Teleport to="body">
      <div v-if="confirmModal" class="confirm-overlay" @click.self="closeConfirmModal">
        <div class="confirm-panel" role="dialog" aria-modal="true" aria-labelledby="confirm-dialog-title">
          <h3 id="confirm-dialog-title" class="confirm-title">确认订单</h3>
          <p class="confirm-order-no">{{ confirmTargetNo }}</p>
          <label class="confirm-field-label" for="estimated-hours-select">预计工作时间</label>
          <div class="confirm-select-wrap">
            <select
              id="estimated-hours-select"
              v-model.number="estimatedHours"
              class="confirm-select"
              :disabled="confirmBusy"
            >
              <option v-for="opt in ESTIMATED_HOUR_OPTIONS" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </option>
            </select>
          </div>
          <p class="confirm-hint">单位：小时。确认后将生成待分配工单。</p>
          <div class="confirm-actions">
            <button type="button" class="confirm-btn ghost" :disabled="confirmBusy" @click="closeConfirmModal">
              取消
            </button>
            <button type="button" class="confirm-btn primary" :disabled="confirmBusy" @click="submitAgentConfirm">
              {{ confirmBusy ? "提交中…" : "确认" }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
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
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.btn-act {
  border-radius: 6px;
  padding: 6px 14px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  border: 1px solid transparent;
}
.btn-act--primary {
  border: none;
  background: #1f6dff;
  color: #fff;
}
.btn-act--primary:active {
  background: #1858cc;
}
.btn-act--danger {
  background: #fff;
  color: #b91c1c;
  border-color: #fecaca;
}
.btn-act--danger:active {
  background: #fef2f2;
}

.order-card__hours {
  margin-top: 6px;
  font-size: 11px;
}

.confirm-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  background: rgba(15, 23, 42, 0.45);
  backdrop-filter: blur(2px);
}

.confirm-panel {
  width: 100%;
  max-width: 340px;
  border-radius: 12px;
  padding: 18px 16px 16px;
  background: #fff;
  box-shadow: 0 12px 40px rgba(15, 23, 42, 0.18);
  border: 1px solid #e2e8f0;
}

.confirm-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
}

.confirm-order-no {
  margin: 8px 0 14px;
  font-size: 12px;
  color: #64748b;
  word-break: break-all;
}

.confirm-field-label {
  display: block;
  margin-bottom: 6px;
  font-size: 12px;
  font-weight: 500;
  color: #334155;
}

.confirm-select-wrap {
  position: relative;
}

.confirm-select-wrap::after {
  content: "";
  position: absolute;
  right: 12px;
  top: 50%;
  width: 0;
  height: 0;
  margin-top: -3px;
  border-left: 5px solid transparent;
  border-right: 5px solid transparent;
  border-top: 6px solid #64748b;
  pointer-events: none;
}

.confirm-select {
  width: 100%;
  box-sizing: border-box;
  appearance: none;
  padding: 10px 36px 10px 12px;
  font-size: 14px;
  color: #0f172a;
  background: #f8fafc;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  outline: none;
  cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s, background 0.15s;
}

.confirm-select:hover:not(:disabled) {
  border-color: #94a3b8;
  background: #fff;
}

.confirm-select:focus {
  border-color: #1f6dff;
  box-shadow: 0 0 0 3px rgba(31, 109, 255, 0.2);
  background: #fff;
}

.confirm-select:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.confirm-hint {
  margin: 10px 0 0;
  font-size: 11px;
  color: #94a3b8;
  line-height: 1.45;
}

.confirm-actions {
  margin-top: 18px;
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

.confirm-btn {
  border-radius: 8px;
  padding: 8px 16px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  border: 1px solid transparent;
}

.confirm-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.confirm-btn.ghost {
  background: #fff;
  color: #475569;
  border-color: #e2e8f0;
}

.confirm-btn.ghost:hover:not(:disabled) {
  background: #f8fafc;
}

.confirm-btn.primary {
  background: #1f6dff;
  color: #fff;
  border: none;
}

.confirm-btn.primary:hover:not(:disabled) {
  background: #1858cc;
}
</style>
