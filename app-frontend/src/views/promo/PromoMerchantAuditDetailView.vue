<script setup>
import { computed, inject, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { requestJson } from "../../api.js";
import "./promo-form.css";

const shell = inject("appShell");
const route = useRoute();
const router = useRouter();
const err = ref("");
const detail = ref(null);
const reviewRemark = ref("");

const auditId = computed(() => Number(route.params.auditId));
const canReview = computed(() => detail.value?.canReview === true);

async function load() {
  err.value = "";
  try {
    const oid = encodeURIComponent(shell.loginOpenid);
    detail.value = await requestJson(`/app-api/biz/merchant-audits/detail?openid=${oid}&auditId=${auditId.value}`);
  } catch (e) {
    err.value = e.message || String(e);
    detail.value = null;
  }
}

async function approve() {
  err.value = "";
  try {
    await requestJson(`/app-api/biz/merchant-audits/${auditId.value}/approve`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        openid: shell.loginOpenid,
        reviewRemark: reviewRemark.value.trim() || null
      })
    });
    window.alert("已通过");
    await load();
  } catch (e) {
    err.value = e.message || String(e);
  }
}

async function reject() {
  err.value = "";
  try {
    await requestJson(`/app-api/biz/merchant-audits/${auditId.value}/reject`, {
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

const FIELD_LABEL_ZH = {
  industryType: "所属行业",
  merchantName: "店铺名称",
  contactName: "联系人",
  contactPhone: "联系电话",
  province: "省",
  city: "市",
  district: "区",
  addressDetail: "详细地址",
  longitude: "经度",
  latitude: "纬度",
  oilUnitPrice: "单价（元/桶）",
  merchantCommission: "商家抽成",
  salesmanId: "业务员",
  linkedMerchantId: "关联商家",
  remark: "说明",
  storeImageUrl: "店铺图片"
};

watch(
  () => [shell.loginOpenid, route.params.auditId],
  () => load()
);
onMounted(load);

function formatPayload(p) {
  if (!p || typeof p !== "object") return [];
  return Object.entries(p).map(([k, v]) => {
    let display = v === null || v === undefined ? "—" : String(v);
    if (k === "storeImageUrl" && typeof v === "string" && v.startsWith("data:")) {
      display = "（已上传图片）";
    }
    if ((k === "salesmanId" || k === "linkedMerchantId") && (v === null || v === undefined || v === "")) {
      display = "无";
    }
    return { label: FIELD_LABEL_ZH[k] || k, value: display };
  });
}
</script>

<template>
  <div class="pf-page">
    <p v-if="err" class="pf-err">{{ err }}</p>
    <template v-if="detail">
      <div class="pf-card">
        <div class="pf-line-strong">审核 #{{ detail.auditId }}</div>
        <div class="pf-line-muted">店铺 #{{ detail.merchantId }} {{ detail.merchantName }}</div>
        <div class="pf-line-muted">状态 {{ detail.status }} · 提交 {{ detail.createTime }}</div>
        <div v-if="detail.reviewTime" class="pf-line-muted">处理 {{ detail.reviewTime }}</div>
        <div v-if="detail.submitRemark" class="pf-line-muted">提交说明 {{ detail.submitRemark }}</div>
        <div v-if="detail.reviewRemark" class="pf-line-muted">审批备注 {{ detail.reviewRemark }}</div>
      </div>

      <h3 class="pf-panel-title" style="margin: 14px 0 8px">修改内容</h3>
      <div class="pf-card">
        <div v-for="(line, i) in formatPayload(detail.payload)" :key="i" class="pf-row">
          <div class="pf-label" style="flex: 0 0 120px">{{ line.label }}</div>
          <div class="pf-field-wrap">
            <span class="pf-muted" style="text-align: right; width: 100%">{{ line.value }}</span>
          </div>
        </div>
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
        <button type="button" class="pf-tool pf-tool--ghost" style="width: 100%" @click="router.push({ name: 'promo-merchant-audits' })">
          返回列表
        </button>
      </div>
    </template>
  </div>
</template>
