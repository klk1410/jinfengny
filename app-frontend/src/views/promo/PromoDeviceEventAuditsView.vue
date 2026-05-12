<script setup>
import { inject, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { requestJson } from "../../api.js";
import "./promo-form.css";
import { auditLikeStatusPillClass } from "../../utils/statusDisplay.js";

const shell = inject("appShell");
const router = useRouter();
const rows = ref([]);
const err = ref("");

async function load() {
  err.value = "";
  try {
    const oid = encodeURIComponent(shell.loginOpenid);
    rows.value = await requestJson(`/app-api/biz/device-event-audits?openid=${oid}`);
  } catch (e) {
    err.value = e.message || String(e);
    rows.value = [];
  }
}

watch(() => shell.loginOpenid, load);
onMounted(load);
</script>

<template>
  <div class="pf-page">
    <p v-if="err" class="pf-err">{{ err }}</p>
    <p class="pf-muted" style="margin: 0 0 12px">
      业务员提交的<strong>设备入库 / 移除 / 报废 / 转移至商家</strong>须经代理（或主端）审批后生效。主端查看全部；代理仅本代理；业务员仅本人提交。
    </p>

    <div class="pf-toolbar">
      <button type="button" class="pf-tool pf-tool--ghost" @click="load">刷新</button>
    </div>

    <div class="pf-card">
      <div v-if="!rows.length" class="pf-muted" style="padding: 12px">暂无审核单</div>
      <div v-else class="dc-stack">
        <button
          v-for="r in rows"
          :key="r.auditId"
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
</style>
