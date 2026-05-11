<script setup>
import { computed, inject, onMounted, ref, watch } from "vue";
import { requestJson } from "../../api.js";

const shell = inject("appShell");
const rows = ref([]);
const err = ref("");
const title = ref("预付款入账");
const amount = ref(100);
const direction = ref("1");
const refNote = ref("");
const merchantId = ref("");
const agentId = ref("1");

const roleCode = computed(() => shell.portal?.roleCode ?? "");
const isMain = computed(() => roleCode.value === "main");
const canCreate = computed(() => roleCode.value === "main" || roleCode.value === "agent");

async function load() {
  err.value = "";
  try {
    const oid = shell.loginOpenid;
    rows.value = await requestJson(`/app-api/promo/prepaids?openid=${encodeURIComponent(oid)}`);
  } catch (e) {
    err.value = e.message || String(e);
    rows.value = [];
  }
}

async function submit() {
  err.value = "";
  try {
    const body = {
      openid: shell.loginOpenid,
      title: title.value.trim(),
      amount: Number(amount.value),
      direction: direction.value,
      refNote: refNote.value.trim() || null
    };
    const mid = merchantId.value.trim();
    if (mid) {
      body.merchantId = Number(mid);
    }
    if (isMain.value) {
      body.agentId = Number(agentId.value);
    }
    await requestJson("/app-api/promo/prepaids", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body)
    });
    await load();
  } catch (e) {
    err.value = e.message || String(e);
  }
}

watch(() => shell.loginOpenid, load);
onMounted(load);
</script>

<template>
  <div class="page">
    <h2 class="page-title">推广 · 预付款</h2>
    <p v-if="err" class="err">{{ err }}</p>

    <div v-if="canCreate" class="card">
      <h3 class="sub">登记流水</h3>
      <div v-if="isMain" class="row">
        <label>代理 ID</label>
        <input v-model="agentId" class="inp" type="text" />
      </div>
      <div class="row">
        <label>关联门店 ID（可选）</label>
        <input v-model="merchantId" class="inp" type="text" placeholder="留空表示代理级" />
      </div>
      <div class="row">
        <label>摘要</label>
        <input v-model="title" class="inp" type="text" />
      </div>
      <div class="row">
        <label>金额（元）</label>
        <input v-model.number="amount" class="inp" type="number" min="0.01" step="0.01" />
      </div>
      <div class="row">
        <label>方向</label>
        <select v-model="direction" class="inp">
          <option value="1">入账</option>
          <option value="2">支出</option>
        </select>
      </div>
      <div class="row">
        <label>备注</label>
        <input v-model="refNote" class="inp" type="text" />
      </div>
      <button type="button" class="btn" @click="submit">保存</button>
    </div>

    <div class="card">
      <h3 class="sub">流水列表</h3>
      <button type="button" class="btn secondary" @click="load">刷新</button>
      <div v-if="!rows.length" class="muted">暂无数据</div>
      <div v-for="(r, i) in rows" :key="i" class="item">
        <div class="line strong">{{ r.title }} · ¥{{ r.amount }} · {{ r.direction }}</div>
        <div class="line muted">
          {{ r.createTime }} · 代理 #{{ r.agentId }} · 门店 {{ r.merchantId ?? "—" }}
        </div>
        <div v-if="r.refNote" class="line">{{ r.refNote }}</div>
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
.sub {
  margin: 0 0 8px;
  font-size: 14px;
  font-weight: 600;
}
.err {
  color: #b91c1c;
  font-size: 13px;
}
.card {
  background: #fff;
  border-radius: 10px;
  padding: 12px;
  margin-bottom: 10px;
  box-shadow: 0 1px 0 rgba(0, 0, 0, 0.04);
}
.row {
  margin-bottom: 10px;
}
.row label {
  display: block;
  font-size: 12px;
  color: #64748b;
  margin-bottom: 4px;
}
.inp {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #d0d7e2;
  border-radius: 8px;
  padding: 8px 10px;
  font-size: 14px;
}
.btn {
  border: none;
  background: #1f6dff;
  color: #fff;
  border-radius: 8px;
  padding: 8px 14px;
  font-size: 13px;
  cursor: pointer;
}
.btn.secondary {
  margin-bottom: 10px;
  background: #e2e8f0;
  color: #334155;
}
.muted {
  color: #64748b;
  font-size: 12px;
}
.item {
  border-top: 1px solid #eef1f6;
  padding: 10px 0;
  font-size: 12px;
}
.item:first-of-type {
  border-top: none;
}
.line {
  margin-top: 4px;
}
.strong {
  font-weight: 600;
}
</style>
