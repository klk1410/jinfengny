<script setup>
import { computed, inject, onMounted, ref, watch } from "vue";
import { requestJson } from "../../api.js";

const shell = inject("appShell");
const rows = ref([]);
const err = ref("");
const amount = ref(100);

const roleCode = computed(() => shell.portal?.roleCode ?? "");
const canApply = computed(() => roleCode.value === "agent");
const canAudit = computed(() => roleCode.value === "main" || roleCode.value === "agent");

async function load() {
  err.value = "";
  try {
    const oid = shell.loginOpenid;
    rows.value = await requestJson(`/app-api/promo/withdraws?openid=${encodeURIComponent(oid)}`);
  } catch (e) {
    err.value = e.message || String(e);
    rows.value = [];
  }
}

async function apply() {
  err.value = "";
  try {
    await requestJson("/app-api/promo/withdraws", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        openid: shell.loginOpenid,
        amount: Number(amount.value)
      })
    });
    await load();
  } catch (e) {
    err.value = e.message || String(e);
  }
}

async function approve(id) {
  err.value = "";
  try {
    const oid = encodeURIComponent(shell.loginOpenid);
    await requestJson(`/app-api/promo/withdraws/${id}/approve?openid=${oid}`, { method: "POST" });
    await load();
  } catch (e) {
    err.value = e.message || String(e);
  }
}

async function reject(id) {
  const remark = window.prompt("驳回原因（可空）") ?? "";
  err.value = "";
  try {
    const q = new URLSearchParams();
    q.set("openid", shell.loginOpenid);
    if (remark) {
      q.set("remark", remark);
    }
    await requestJson(`/app-api/promo/withdraws/${id}/reject?${q.toString()}`, { method: "POST" });
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
    <h2 class="page-title">推广 · 提现管理</h2>
    <p v-if="err" class="err">{{ err }}</p>

    <div v-if="canApply" class="card">
      <h3 class="sub">申请提现</h3>
      <div class="row">
        <label>金额（元）</label>
        <input v-model.number="amount" class="inp" type="number" min="0.01" step="0.01" />
      </div>
      <button type="button" class="btn" @click="apply">提交申请</button>
    </div>

    <div class="card">
      <h3 class="sub">记录</h3>
      <button type="button" class="btn secondary" @click="load">刷新</button>
      <div v-if="!rows.length" class="muted">暂无数据</div>
      <div v-for="(r, i) in rows" :key="i" class="item">
        <div class="line strong">#{{ r.withdrawId }} ¥{{ r.amount }} · {{ r.status }}</div>
        <div class="line muted">{{ r.createTime }} · 申请人 {{ r.applicantOpenid || "—" }}</div>
        <div v-if="r.auditRemark" class="line">{{ r.auditRemark }}</div>
        <div v-if="canAudit && r.statusCode === '0'" class="acts">
          <button type="button" class="mini ok" @click="approve(r.withdrawId)">通过</button>
          <button type="button" class="mini bad" @click="reject(r.withdrawId)">驳回</button>
        </div>
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
.acts {
  margin-top: 8px;
  display: flex;
  gap: 8px;
}
.mini {
  border: none;
  border-radius: 6px;
  padding: 6px 12px;
  font-size: 12px;
  cursor: pointer;
}
.mini.ok {
  background: #dcfce7;
  color: #166534;
}
.mini.bad {
  background: #fee2e2;
  color: #991b1b;
}
</style>
