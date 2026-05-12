<script setup>
import { computed, inject, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { requestJson } from "../../api.js";
import "./promo-form.css";
import { auditLikeStatusPillClass } from "../../utils/statusDisplay.js";

const shell = inject("appShell");
const router = useRouter();
const route = useRoute();

const roleCode = computed(() => shell.portal?.roleCode ?? "");
const canSeeDeviceAudits = computed(() => roleCode.value === "main" || roleCode.value === "agent");

const auditTab = ref("shop");
const merchantRows = ref([]);
const deviceRows = ref([]);
const err = ref("");

function syncTabFromRoute() {
  if (canSeeDeviceAudits.value && route.query.tab === "device") {
    auditTab.value = "device";
  } else {
    auditTab.value = "shop";
  }
}

async function load() {
  err.value = "";
  const oid = encodeURIComponent(shell.loginOpenid);
  try {
    merchantRows.value = (await requestJson(`/app-api/biz/merchant-audits?openid=${oid}`)) || [];
    if (canSeeDeviceAudits.value && auditTab.value === "device") {
      deviceRows.value = (await requestJson(`/app-api/biz/device-event-audits?openid=${oid}`)) || [];
    } else {
      deviceRows.value = [];
    }
  } catch (e) {
    err.value = e.message || String(e);
    merchantRows.value = [];
    deviceRows.value = [];
  }
}

function selectTab(tab) {
  if (!canSeeDeviceAudits.value) {
    return;
  }
  router.replace({
    name: "promo-merchant-audits",
    query: tab === "device" ? { tab: "device" } : {}
  });
}

watch(
  () => [route.query.tab, shell.loginOpenid, canSeeDeviceAudits.value],
  () => {
    syncTabFromRoute();
    load();
  },
  { immediate: true }
);
</script>

<template>
  <div class="pf-page">
    <p v-if="err" class="pf-err">{{ err }}</p>

    <div v-if="canSeeDeviceAudits" class="audit-tabs" role="tablist">
      <button
        type="button"
        role="tab"
        class="audit-tab"
        :class="{ 'audit-tab--active': auditTab === 'shop' }"
        :aria-selected="auditTab === 'shop'"
        @click="selectTab('shop')"
      >
        店铺审核
      </button>
      <button
        type="button"
        role="tab"
        class="audit-tab"
        :class="{ 'audit-tab--active': auditTab === 'device' }"
        :aria-selected="auditTab === 'device'"
        @click="selectTab('device')"
      >
        设备操作审核
      </button>
    </div>

    <p v-if="auditTab === 'shop'" class="pf-muted intro">
      店铺资料修改与新建店铺：主端查看全部；代理仅本代理；业务员仅本人发起的记录；商家仅本人提交的审核。
    </p>
    <p v-else class="pf-muted intro">
      业务员提交的入库 / 移除 / 报废 / 调至门店等设备操作，仅主端与代理可在此审批（业务员仅能通过提交接口发起，不可查看此列表）。
    </p>

    <div class="pf-toolbar">
      <button type="button" class="pf-tool pf-tool--ghost" @click="load">刷新</button>
    </div>

    <div v-if="auditTab === 'shop'" class="pf-card">
      <div v-if="!merchantRows.length" class="pf-muted" style="padding: 12px">暂无店铺审核单</div>
      <div v-else class="dc-stack">
        <button
          v-for="r in merchantRows"
          :key="'m-' + r.auditId"
          type="button"
          class="dc-card dc-card--white pf-item--link"
          @click="router.push({ name: 'promo-merchant-audit-detail', params: { auditId: String(r.auditId) } })"
        >
          <div class="audit-head">
            <span class="pf-line-strong">
              <span v-if="r.auditKind === 'C'" class="audit-kind-pill">新建</span>
              <span v-else class="audit-kind-pill audit-kind-pill--muted">修改</span>
              #{{ r.auditId }}
              <template v-if="r.merchantId != null"> 店铺 #{{ r.merchantId }}</template>
              {{ r.merchantName || "—" }}
            </span>
            <span :class="auditLikeStatusPillClass(r.statusCode)">{{ r.status }}</span>
          </div>
          <div class="pf-line-muted">{{ r.createTime }}</div>
          <div v-if="r.submitterSalesmanName" class="pf-line-muted">业务员 {{ r.submitterSalesmanName }}</div>
          <div v-if="r.submitRemark" class="pf-line-muted">说明 {{ r.submitRemark }}</div>
        </button>
      </div>
    </div>

    <div v-else-if="canSeeDeviceAudits" class="pf-card">
      <div v-if="!deviceRows.length" class="pf-muted" style="padding: 12px">暂无设备操作审核单</div>
      <div v-else class="dc-stack">
        <button
          v-for="r in deviceRows"
          :key="'d-' + r.auditId"
          type="button"
          class="dc-card dc-card--white pf-item--link"
          @click="router.push({ name: 'promo-device-event-audit-detail', params: { auditId: String(r.auditId) } })"
        >
          <div class="audit-head">
            <span class="pf-line-strong">#{{ r.auditId }} {{ r.deviceNo }} · {{ r.eventType }}</span>
            <span :class="auditLikeStatusPillClass(r.statusCode)">{{ r.status }}</span>
          </div>
          <div class="pf-line-muted">{{ r.createTime }}</div>
          <div v-if="r.submitterSalesmanName" class="pf-line-muted">业务员 {{ r.submitterSalesmanName }}</div>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.intro {
  margin: 0 0 12px;
  line-height: 1.5;
}
.audit-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.audit-tab {
  flex: 1;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 10px 12px;
  font-size: 13px;
  font-weight: 500;
  color: #475569;
  background: #f8fafc;
  cursor: pointer;
  transition:
    background 0.15s,
    border-color 0.15s,
    color 0.15s;
}
.audit-tab--active {
  border-color: #1f6dff;
  background: #eff6ff;
  color: #1e40af;
  font-weight: 600;
}
.pf-item--link {
  width: 100%;
  text-align: left;
  border: none;
  background: transparent;
  cursor: pointer;
  display: block;
}
.audit-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
}
.audit-kind-pill {
  display: inline-block;
  margin-right: 6px;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  color: #1e40af;
  background: #dbeafe;
  vertical-align: middle;
}
.audit-kind-pill--muted {
  color: #64748b;
  background: #f1f5f9;
}
</style>
