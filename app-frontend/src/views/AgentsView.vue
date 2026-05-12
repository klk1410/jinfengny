<script setup>
import { computed, inject, onMounted, ref, watch } from "vue";
import { requestJson } from "../api.js";
import { entityOnOffPillClass } from "../utils/statusDisplay.js";

const shell = inject("appShell");
const rows = ref([]);
const err = ref("");
const roleCode = computed(() => shell.portal?.roleCode ?? "");
const showNewLink = computed(() => roleCode.value === "main");

async function load() {
  err.value = "";
  try {
    const oid = shell.loginOpenid;
    rows.value = await requestJson(`/app-api/biz/agents?openid=${encodeURIComponent(oid)}`);
  } catch (e) {
    err.value = e.message || String(e);
    rows.value = [];
  }
}

watch(() => shell.loginOpenid, load);
onMounted(load);
</script>

<template>
  <div class="page">
    <h2 class="page-title">代理</h2>
    <p v-if="showNewLink" class="subnav">
      <router-link class="subnav-link" :to="{ name: 'agent-new' }">新增代理</router-link>
    </p>
    <p v-if="err" class="err">{{ err }}</p>
    <div class="card">
      <div v-if="!rows.length" class="muted">暂无数据</div>
      <div v-else class="dc-stack">
        <article v-for="(a, i) in rows" :key="i" class="dc-card dc-card--white">
          <div class="row-head">
            <span class="line strong">#{{ a.agentId }} {{ a.agentName }}</span>
            <span :class="entityOnOffPillClass(a.statusCode)">{{ a.status }}</span>
          </div>
          <div class="line">{{ a.contactName }} {{ a.contactPhone }}</div>
          <div class="line muted">{{ a.province }} {{ a.city }}</div>
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
.subnav {
  margin: -4px 0 10px;
  font-size: 13px;
}
.subnav-link {
  color: #2563eb;
  text-decoration: none;
}
.subnav-link:hover {
  text-decoration: underline;
}
.err {
  color: #b91c1c;
  font-size: 13px;
}
.card {
  background: #fff;
  border-radius: 10px;
  padding: 12px;
  box-shadow: 0 1px 0 rgba(0, 0, 0, 0.04);
}
.muted {
  color: #64748b;
  font-size: 12px;
}
.line {
  margin-top: 4px;
}
.line:first-child {
  margin-top: 0;
}
.strong {
  font-weight: 600;
}
.row-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
}
</style>
