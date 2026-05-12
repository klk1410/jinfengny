<script setup>
import { computed, inject, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { requestJson } from "../api.js";

const shell = inject("appShell");
const route = useRoute();
const router = useRouter();

const err = ref("");
const loading = ref(true);
const payload = ref(null);

const orderNo = computed(() => String(route.params.orderNo || ""));

async function load() {
  err.value = "";
  loading.value = true;
  payload.value = null;
  const no = orderNo.value;
  if (!no) {
    loading.value = false;
    return;
  }
  try {
    const oid = encodeURIComponent(shell.loginOpenid);
    payload.value = await requestJson(`/app-api/order/timeline/${encodeURIComponent(no)}?openid=${oid}`);
  } catch (e) {
    err.value = e.message || String(e);
  } finally {
    loading.value = false;
  }
}

watch(
  () => [shell.loginOpenid, route.params.orderNo],
  () => load(),
  { deep: true }
);
onMounted(load);

function stepDotClass(code) {
  const c = String(code || "");
  if (c === "order_create") {
    return "tl-dot tl-dot--create";
  }
  if (c === "order_confirm") {
    return "tl-dot tl-dot--confirm";
  }
  if (c === "work_grab") {
    return "tl-dot tl-dot--grab";
  }
  if (c === "work_assign") {
    return "tl-dot tl-dot--assign";
  }
  if (c === "work_finish") {
    return "tl-dot tl-dot--finish";
  }
  if (c === "order_cancel") {
    return "tl-dot tl-dot--cancel";
  }
  return "tl-dot tl-dot--muted";
}

function sourceHint(src) {
  if (src === "inferred") {
    return "以下为根据订单/工单时间推断的流程（升级前数据无明细日志）";
  }
  return "";
}
</script>

<template>
  <div class="page">
    <div class="toolbar">
      <button type="button" class="back" @click="router.back()">返回</button>
      <span class="toolbar-no">{{ orderNo }}</span>
    </div>

    <p v-if="err" class="err">{{ err }}</p>
    <p v-if="loading" class="muted">加载中…</p>

    <template v-else-if="payload">
      <p v-if="sourceHint(payload.source)" class="src-hint">{{ sourceHint(payload.source) }}</p>

      <div class="card">
        <h3 class="sub">下单流程</h3>
        <div v-if="!payload.steps?.length" class="muted">暂无节点</div>
        <div v-else class="timeline">
          <div v-for="(st, i) in payload.steps" :key="i" class="tl-row">
            <div class="tl-axis">
              <span :class="stepDotClass(st.eventCode)" />
              <span v-if="i < payload.steps.length - 1" class="tl-line" />
            </div>
            <div class="tl-body">
              <div class="tl-time">{{ st.operationTime }}</div>
              <div class="tl-title">{{ st.title }}</div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.page {
  padding-bottom: 16px;
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}
.back {
  border: 1px solid #cbd5e1;
  background: #fff;
  border-radius: 8px;
  padding: 6px 12px;
  font-size: 13px;
  cursor: pointer;
  color: #334155;
}
.toolbar-no {
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
  word-break: break-all;
}
.err {
  color: #b91c1c;
  font-size: 13px;
}
.muted {
  color: #64748b;
  font-size: 12px;
}
.src-hint {
  font-size: 11px;
  color: #b45309;
  line-height: 1.45;
  margin: 0 0 10px;
  padding: 8px 10px;
  background: #fffbeb;
  border-radius: 8px;
  border: 1px solid #fcd34d;
}
.card {
  background: #fff;
  border-radius: 10px;
  padding: 12px;
  box-shadow: 0 1px 0 rgba(0, 0, 0, 0.04);
}
.sub {
  margin: 0 0 12px;
  font-size: 14px;
}
.timeline {
  display: flex;
  flex-direction: column;
  gap: 0;
}
.tl-row {
  display: flex;
  gap: 12px;
  align-items: stretch;
}
.tl-axis {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 18px;
  flex-shrink: 0;
}
.tl-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 2px solid #fff;
  box-shadow: 0 0 0 1px #e2e8f0;
  flex-shrink: 0;
}
.tl-dot--create {
  background: #64748b;
}
.tl-dot--confirm {
  background: #2563eb;
}
.tl-dot--grab {
  background: #0d9488;
}
.tl-dot--assign {
  background: #d97706;
}
.tl-dot--finish {
  background: #059669;
}
.tl-dot--cancel {
  background: #dc2626;
}
.tl-dot--muted {
  background: #94a3b8;
}
.tl-line {
  flex: 1;
  width: 2px;
  min-height: 12px;
  margin: 2px 0;
  background: linear-gradient(#e2e8f0, #e2e8f0);
  border-radius: 1px;
}
.tl-body {
  flex: 1;
  padding-bottom: 14px;
}
.tl-time {
  font-size: 11px;
  color: #64748b;
  margin-bottom: 4px;
}
.tl-title {
  font-size: 13px;
  color: #0f172a;
  line-height: 1.45;
  font-weight: 500;
}
</style>
