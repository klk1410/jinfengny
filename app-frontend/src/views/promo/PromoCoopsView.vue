<script setup>
import { inject, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { requestJson } from "../../api.js";
import "./promo-form.css";
import { coopStatusPillClass } from "../../utils/statusDisplay.js";

const shell = inject("appShell");
const router = useRouter();
const rows = ref([]);
const err = ref("");

async function load() {
  err.value = "";
  try {
    const oid = shell.loginOpenid;
    rows.value = await requestJson(`/app-api/promo/coops?openid=${encodeURIComponent(oid)}`);
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

    <div class="pf-toolbar">
      <button type="button" class="pf-tool" @click="router.push({ name: 'promo-coop-new' })">新增合作</button>
      <button type="button" class="pf-tool pf-tool--ghost" @click="load">刷新</button>
      <button type="button" class="pf-tool pf-tool--ghost" @click="router.push({ name: 'promo-stores' })">店铺</button>
    </div>

    <div class="pf-card">
      <div v-if="!rows.length" class="pf-muted" style="padding: 12px">暂无数据</div>
      <div v-else class="dc-stack">
        <article v-for="(r, i) in rows" :key="i" class="dc-card dc-card--white">
          <div class="coop-head">
            <span class="pf-line-strong">#{{ r.coopId }} {{ r.partnerName }}</span>
            <span :class="coopStatusPillClass(r.statusCode)">{{ r.status }}</span>
          </div>
          <div class="pf-line-muted">{{ r.contactName || "—" }} {{ r.contactPhone || "" }}</div>
          <div class="pf-line-muted">代理 #{{ r.agentId }} · {{ r.createTime }}</div>
          <div v-if="r.remark" class="pf-line-muted">{{ r.remark }}</div>
        </article>
      </div>
    </div>
  </div>
</template>

<style scoped>
.coop-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
}
</style>
