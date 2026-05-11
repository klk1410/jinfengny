<script setup>
import { computed, inject, onMounted, ref, watch } from "vue";
import { requestJson } from "../../api.js";
import "./promo-form.css";

const shell = inject("appShell");
const rows = ref([]);
const merchants = ref([]);
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

async function loadMerchants() {
  const oid = encodeURIComponent(shell.loginOpenid);
  merchants.value = await requestJson(`/app-api/biz/merchants?openid=${oid}`);
}

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
  if (!title.value.trim()) {
    err.value = "请填写摘要";
    return;
  }
  if (!amount.value || Number(amount.value) <= 0) {
    err.value = "请填写有效金额";
    return;
  }
  try {
    const body = {
      openid: shell.loginOpenid,
      title: title.value.trim(),
      amount: Number(amount.value),
      direction: direction.value,
      refNote: refNote.value.trim() || null
    };
    if (merchantId.value) {
      body.merchantId = Number(merchantId.value);
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

watch(() => shell.loginOpenid, () => {
  load();
  loadMerchants().catch(() => {});
});

onMounted(() => {
  load();
  loadMerchants().catch((e) => {
    err.value = e.message || String(e);
  });
});
</script>

<template>
  <div class="pf-page">
    <p v-if="err" class="pf-err">{{ err }}</p>

    <div v-if="canCreate" class="pf-card">
      <div v-if="isMain" class="pf-row">
        <div class="pf-label req">代理</div>
        <div class="pf-field-wrap">
          <input v-model="agentId" class="pf-field" type="text" placeholder="主端填写代理 ID" />
        </div>
      </div>

      <div class="pf-row">
        <div class="pf-label">关联门店</div>
        <div class="pf-field-wrap">
          <select v-model="merchantId" class="pf-field">
            <option value="">选择门店（可选）</option>
            <option v-for="m in merchants" :key="m.merchantId" :value="String(m.merchantId)">
              {{ m.merchantName }}
            </option>
          </select>
        </div>
        <span class="pf-chevron">›</span>
      </div>

      <div class="pf-row">
        <div class="pf-label req">摘要</div>
        <div class="pf-field-wrap">
          <input v-model="title" class="pf-field" type="text" placeholder="如：预付款入账" />
        </div>
      </div>

      <div class="pf-row">
        <div class="pf-label req">金额</div>
        <div class="pf-field-wrap">
          <input v-model.number="amount" class="pf-field" type="number" min="0.01" step="0.01" placeholder="元" />
        </div>
      </div>

      <div class="pf-row">
        <div class="pf-label req">方向</div>
        <div class="pf-field-wrap">
          <select v-model="direction" class="pf-field">
            <option value="1">入账</option>
            <option value="2">支出</option>
          </select>
        </div>
        <span class="pf-chevron">▼</span>
      </div>

      <div class="pf-row">
        <div class="pf-label">备注</div>
        <div class="pf-field-wrap">
          <input v-model="refNote" class="pf-field" type="text" placeholder="请填写备注" />
        </div>
      </div>
    </div>

    <div v-if="canCreate" class="pf-footer">
      <button type="button" class="pf-submit" @click="submit">提交</button>
    </div>

    <div class="pf-panel">
      <h3 class="pf-panel-title">流水列表</h3>
      <button type="button" class="pf-refresh" @click="load">刷新</button>
      <div v-if="!rows.length" class="pf-muted">暂无数据</div>
      <div v-for="(r, i) in rows" :key="i" class="pf-item">
        <div class="pf-line-strong">{{ r.title }} · ¥{{ r.amount }} · {{ r.direction }}</div>
        <div class="pf-line-muted">
          {{ r.createTime }} · 代理 #{{ r.agentId }} · 门店 {{ r.merchantId ?? "—" }}
        </div>
        <div v-if="r.refNote" class="pf-line-muted">{{ r.refNote }}</div>
      </div>
    </div>
  </div>
</template>
