<script setup>
import { computed, inject, onMounted, ref, watch } from "vue";
import { requestJson } from "../../api.js";
import "./promo-form.css";

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
  if (!amount.value || Number(amount.value) <= 0) {
    err.value = "请填写有效金额";
    return;
  }
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
  <div class="pf-page">
    <p v-if="err" class="pf-err">{{ err }}</p>

    <div v-if="canApply" class="pf-card">
      <div class="pf-row">
        <div class="pf-label req">提现金额</div>
        <div class="pf-field-wrap">
          <input v-model.number="amount" class="pf-field" type="number" min="0.01" step="0.01" placeholder="元" />
        </div>
      </div>
    </div>

    <div v-if="canApply" class="pf-footer">
      <button type="button" class="pf-submit" @click="apply">提交申请</button>
    </div>

    <div class="pf-panel">
      <h3 class="pf-panel-title">记录</h3>
      <button type="button" class="pf-refresh" @click="load">刷新</button>
      <div v-if="!rows.length" class="pf-muted">暂无数据</div>
      <div v-for="(r, i) in rows" :key="i" class="pf-item">
        <div class="pf-line-strong">#{{ r.withdrawId }} ¥{{ r.amount }} · {{ r.status }}</div>
        <div class="pf-line-muted">{{ r.createTime }} · 申请人 {{ r.applicantOpenid || "—" }}</div>
        <div v-if="r.auditRemark" class="pf-line-muted">{{ r.auditRemark }}</div>
        <div v-if="canAudit && r.statusCode === '0'" class="pf-mini-actions">
          <button type="button" class="pf-mini ok" @click="approve(r.withdrawId)">通过</button>
          <button type="button" class="pf-mini bad" @click="reject(r.withdrawId)">驳回</button>
        </div>
      </div>
    </div>
  </div>
</template>
