<script setup>
import { computed, inject, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { requestJson } from "../../api.js";
import "./promo-form.css";
import { entityOnOffPillClass } from "../../utils/statusDisplay.js";

const shell = inject("appShell");
const router = useRouter();
const roleCode = computed(() => shell.portal?.roleCode ?? "");
const canCreateStore = computed(() => roleCode.value !== "merchant");
const rows = ref([]);
const err = ref("");

async function load() {
  err.value = "";
  try {
    const oid = shell.loginOpenid;
    rows.value = await requestJson(`/app-api/biz/merchants?openid=${encodeURIComponent(oid)}`);
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
      <button v-if="canCreateStore" type="button" class="pf-tool" @click="router.push({ name: 'promo-store-new' })">
        新增店铺
      </button>
      <button
        v-if="roleCode === 'main' || roleCode === 'agent' || roleCode === 'sales'"
        type="button"
        class="pf-tool pf-tool--ghost"
        @click="router.push({ name: 'promo-merchant-audits' })"
      >
        店铺审核
      </button>
      <button type="button" class="pf-tool pf-tool--ghost" @click="load">刷新</button>
    </div>

    <div class="store-list-wrap">
      <div v-if="!rows.length" class="pf-muted" style="padding: 12px">暂无数据</div>
      <div v-else class="dc-stack">
        <button
          v-for="(m, i) in rows"
          :key="i"
          type="button"
          class="dc-card dc-card--white store-card-hit"
          @click="router.push({ name: 'promo-store-detail', params: { merchantId: String(m.merchantId) } })"
        >
          <div class="store-head">
            <span class="pf-line-strong">#{{ m.merchantId }} {{ m.merchantName }}</span>
            <span :class="entityOnOffPillClass(m.statusCode)">{{ m.status }}</span>
          </div>
          <div class="pf-line-muted">{{ m.contactName }} {{ m.contactPhone }}</div>
          <div class="pf-line-muted">{{ m.city }} · {{ m.agentName }} · {{ m.salesmanName || "—" }}</div>
          <div class="pf-line-muted amt-line">单价 ¥{{ m.oilUnitPrice }} · 欠费 ¥{{ m.arrearsAmount }}</div>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.store-list-wrap {
  background: #fff;
  border-radius: 12px;
  padding: 12px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 0 rgba(0, 0, 0, 0.04);
  box-sizing: border-box;
}
.store-card-hit {
  text-align: left;
}
.store-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
}
.amt-line {
  color: #475569;
}
</style>
