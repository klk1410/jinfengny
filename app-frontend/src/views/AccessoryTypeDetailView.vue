<script setup>
import { inject, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { requestJson } from "../api.js";

const shell = inject("appShell");
const route = useRoute();
const router = useRouter();
const rows = ref([]);
const err = ref("");
const typeId = ref(null);
const typeName = ref("");

async function load() {
  err.value = "";
  const tid = Number(route.params.typeId);
  typeId.value = tid;
  if (!tid) {
    err.value = "种类无效";
    rows.value = [];
    return;
  }
  try {
    const oid = encodeURIComponent(shell.loginOpenid);
    const list = await requestJson(`/app-api/biz/accessories?openid=${oid}&typeId=${tid}`);
    rows.value = list || [];
    typeName.value = rows.value.length ? rows.value[0].typeName : `种类 #${tid}`;
  } catch (e) {
    err.value = e.message || String(e);
    rows.value = [];
  }
}

function back() {
  router.push({ name: "accessories" });
}

watch(() => shell.loginOpenid, load);
watch(
  () => route.params.typeId,
  () => {
    load();
  }
);
onMounted(load);
</script>

<template>
  <div class="page">
    <h2 class="page-title">{{ typeName || "配件明细" }}</h2>
    <p v-if="err" class="err">{{ err }}</p>
    <button type="button" class="link-back" @click="back">← 返回汇总</button>
    <div class="card">
      <div v-if="!rows.length" class="muted">暂无该种类入库记录</div>
      <div v-else class="dc-stack">
        <article v-for="(r, i) in rows" :key="i" class="dc-card dc-card--white">
          <div class="line strong">数量 {{ r.qty }} · 入库成本 ¥{{ r.inboundCost }}</div>
          <div v-if="r.accCode" class="line muted">编号 {{ r.accCode }}</div>
          <div class="line muted">操作人 {{ r.operatorLabel || "—" }}</div>
          <div class="line muted">门店 {{ r.merchantName || "代理级" }} · 代理 #{{ r.agentId }}</div>
          <div v-if="r.remark" class="line muted">{{ r.remark }}</div>
          <div class="line muted">{{ r.createTime }}</div>
        </article>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-title {
  margin: 0 0 6px;
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
  margin-top: 8px;
  box-shadow: 0 1px 0 rgba(0, 0, 0, 0.04);
}
.line {
  margin-top: 4px;
}
.strong {
  font-weight: 600;
}
.link-back {
  border: none;
  background: none;
  color: #1f6dff;
  font-size: 13px;
  cursor: pointer;
  padding: 0;
}
</style>
