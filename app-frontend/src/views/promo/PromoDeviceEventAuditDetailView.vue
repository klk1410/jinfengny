<script setup>
import { computed, inject, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { requestJson } from "../../api.js";
import "./promo-form.css";
import { auditLikeStatusPillClass } from "../../utils/statusDisplay.js";

const shell = inject("appShell");
const route = useRoute();
const router = useRouter();
const err = ref("");
const detail = ref(null);
const reviewRemark = ref("");

const auditId = computed(() => Number(route.params.auditId));
const canReview = computed(() => detail.value?.canReview === true);

const FIELD_LABEL_ZH = {
  eventType: "操作类型",
  deviceNo: "设备编号",
  merchantId: "目标门店 ID",
  remark: "备注",
  addMode: "入库方式",
  agentId: "代理 ID",
  openid: "提交账号"
};

const EVENT_TYPE_ZH = {
  A: "新增入库",
  R: "移除回库",
  S: "报废",
  T: "转移至商家"
};

async function load() {
  err.value = "";
  try {
    const oid = encodeURIComponent(shell.loginOpenid);
    detail.value = await requestJson(`/app-api/biz/device-event-audits/detail?openid=${oid}&auditId=${auditId.value}`);
  } catch (e) {
    err.value = e.message || String(e);
    detail.value = null;
  }
}

async function approve() {
  err.value = "";
  try {
    await requestJson(`/app-api/biz/device-event-audits/${auditId.value}/approve`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        openid: shell.loginOpenid,
        reviewRemark: reviewRemark.value.trim() || null
      })
    });
    window.alert("已通过，设备台账已更新");
    await load();
  } catch (e) {
    err.value = e.message || String(e);
  }
}

async function reject() {
  err.value = "";
  try {
    await requestJson(`/app-api/biz/device-event-audits/${auditId.value}/reject`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        openid: shell.loginOpenid,
        reviewRemark: reviewRemark.value.trim() || null
      })
    });
    window.alert("已驳回");
    await load();
  } catch (e) {
    err.value = e.message || String(e);
  }
}

watch(
  () => [shell.loginOpenid, route.params.auditId],
  () => load()
);
onMounted(load);

function formatPayload(p) {
  if (!p || typeof p !== "object") return [];
  return Object.entries(p).map(([k, v]) => {
    let display = v === null || v === undefined ? "—" : String(v);
    if (k === "eventType" && EVENT_TYPE_ZH[v]) {
      display = `${EVENT_TYPE_ZH[v]}（${v}）`;
    }
    if (k === "openid") {
      display = String(v).length > 12 ? `${String(v).slice(0, 8)}…` : String(v);
    }
    return { label: FIELD_LABEL_ZH[k] || k, value: display };
  });
}
</script>

<template>
  <div class="pf-page">
    <p v-if="err" class="pf-err">{{ err }}</p>
    <template v-if="detail">
      <div class="pf-card sum-card">
        <div class="sum-title">
          <span class="sum-h">设备审核 #{{ detail.auditId }}</span>
          <span :class="auditLikeStatusPillClass(detail.statusCode)">{{ detail.status }}</span>
        </div>
        <div class="sum-grid">
          <div class="sum-cell">
            <span class="sum-k">设备编号</span>
            <span class="sum-v">{{ detail.deviceNo }}</span>
          </div>
          <div class="sum-cell">
            <span class="sum-k">操作类型</span>
            <span class="sum-v">{{ detail.eventType }}</span>
          </div>
          <div class="sum-cell">
            <span class="sum-k">提交时间</span>
            <span class="sum-v">{{ detail.createTime }}</span>
          </div>
          <div v-if="detail.submitterSalesmanName" class="sum-cell">
            <span class="sum-k">业务员</span>
            <span class="sum-v">{{ detail.submitterSalesmanName }}</span>
          </div>
          <div v-if="detail.reviewTime" class="sum-cell">
            <span class="sum-k">处理时间</span>
            <span class="sum-v">{{ detail.reviewTime }}</span>
          </div>
          <div v-if="detail.submitRemark" class="sum-cell sum-cell--full">
            <span class="sum-k">提交备注</span>
            <span class="sum-v sum-v--wrap">{{ detail.submitRemark }}</span>
          </div>
          <div v-if="detail.reviewRemark" class="sum-cell sum-cell--full">
            <span class="sum-k">审批备注</span>
            <span class="sum-v sum-v--wrap">{{ detail.reviewRemark }}</span>
          </div>
        </div>
      </div>

      <h3 class="pf-panel-title" style="margin: 14px 0 8px">申请内容</h3>
      <div class="dc-stack">
        <article v-for="(line, i) in formatPayload(detail.payload)" :key="i" class="dc-card dc-card--white payload-line">
          <div class="payload-k">{{ line.label }}</div>
          <div class="payload-v">{{ line.value }}</div>
        </article>
      </div>

      <div v-if="canReview" class="pf-card" style="margin-top: 12px">
        <div class="pf-row">
          <div class="pf-label">审批备注</div>
          <div class="pf-field-wrap">
            <input v-model="reviewRemark" class="pf-field" type="text" placeholder="选填" />
          </div>
        </div>
      </div>

      <div v-if="canReview" class="pf-footer" style="display: flex; gap: 10px">
        <button type="button" class="pf-submit" style="flex: 1" @click="approve">通过</button>
        <button type="button" class="pf-tool" style="flex: 1; padding: 14px" @click="reject">驳回</button>
      </div>

      <div class="pf-footer">
        <button
          type="button"
          class="pf-tool pf-tool--ghost"
          style="width: 100%"
          @click="router.push({ name: 'promo-merchant-audits', query: { tab: 'device' } })"
        >
          返回列表
        </button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.sum-card {
  padding-top: 14px;
}
.sum-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.sum-h {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}
.sum-grid {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.sum-cell {
  display: grid;
  grid-template-columns: 88px 1fr;
  gap: 10px;
  align-items: start;
  font-size: 13px;
}
.sum-cell--full {
  grid-template-columns: 88px 1fr;
}
.sum-k {
  color: #64748b;
  line-height: 1.45;
}
.sum-v {
  color: #0f172a;
  font-weight: 500;
  text-align: right;
  word-break: break-word;
  line-height: 1.45;
}
.sum-v--wrap {
  white-space: pre-wrap;
}
.payload-line {
  display: grid;
  grid-template-columns: 100px 1fr;
  gap: 12px;
  font-size: 13px;
  align-items: start;
}
.payload-k {
  color: #64748b;
  line-height: 1.5;
}
.payload-v {
  color: #0f172a;
  font-weight: 500;
  text-align: right;
  word-break: break-word;
  line-height: 1.5;
}
</style>
