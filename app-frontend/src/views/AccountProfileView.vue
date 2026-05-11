<script setup>
import { inject, onMounted, ref, watch } from "vue";
import { requestJson } from "../api.js";
import "./promo/promo-form.css";

const shell = inject("appShell");
const err = ref("");
const base = ref({});
const org = ref({});

async function load() {
  err.value = "";
  try {
    const oid = shell.loginOpenid;
    const data = await requestJson(`/app-api/biz/account/profile?openid=${encodeURIComponent(oid)}`);
    base.value = {
      roleName: data.roleName || "",
      openid: data.openid || "",
      bindTime: data.bindTime || ""
    };
    org.value = data.orgInfo || {};
  } catch (e) {
    err.value = e.message || String(e);
    base.value = {};
    org.value = {};
  }
}

function copyOpenid() {
  const text = base.value.openid || "";
  if (!text) return;
  navigator.clipboard.writeText(text).then(
    () => window.alert("openid 已复制"),
    () => window.alert("复制失败，请手工复制")
  );
}

watch(() => shell.loginOpenid, load);
onMounted(load);
</script>

<template>
  <div class="pf-page">
    <p v-if="err" class="pf-err">{{ err }}</p>

    <div class="pf-panel" style="margin-top: 0">
      <h3 class="pf-panel-title">基础信息卡</h3>
      <div class="pf-item">
        <div class="pf-line-strong">当前角色</div>
        <div class="pf-line-muted">{{ base.roleName || "—" }}</div>
      </div>
      <div class="pf-item">
        <div class="pf-line-strong">openid</div>
        <div class="pf-line-muted" style="word-break: break-all">{{ base.openid || "—" }}</div>
        <button type="button" class="pf-tool pf-tool--ghost" style="margin-top: 8px" @click="copyOpenid">复制</button>
      </div>
      <div class="pf-item">
        <div class="pf-line-strong">绑定时间（可选）</div>
        <div class="pf-line-muted">{{ base.bindTime || "—" }}</div>
      </div>
    </div>

    <div class="pf-panel">
      <h3 class="pf-panel-title">组织信息卡</h3>

      <template v-if="base.roleName === '代理'">
        <div class="pf-item"><div class="pf-line-strong">代理名称</div><div class="pf-line-muted">{{ org.agentName || "—" }}</div></div>
        <div class="pf-item"><div class="pf-line-strong">联系人</div><div class="pf-line-muted">{{ org.contactName || "—" }}</div></div>
        <div class="pf-item"><div class="pf-line-strong">电话</div><div class="pf-line-muted">{{ org.contactPhone || "—" }}</div></div>
        <div class="pf-item"><div class="pf-line-strong">区域</div><div class="pf-line-muted">{{ org.region || "—" }}</div></div>
      </template>

      <template v-else-if="base.roleName === '业务员'">
        <div class="pf-item"><div class="pf-line-strong">姓名</div><div class="pf-line-muted">{{ org.salesmanName || "—" }}</div></div>
        <div class="pf-item"><div class="pf-line-strong">电话</div><div class="pf-line-muted">{{ org.phone || "—" }}</div></div>
        <div class="pf-item"><div class="pf-line-strong">所属代理</div><div class="pf-line-muted">{{ org.agentName || "—" }}</div></div>
      </template>

      <template v-else-if="base.roleName === '商家'">
        <div class="pf-item"><div class="pf-line-strong">门店名</div><div class="pf-line-muted">{{ org.merchantName || "—" }}</div></div>
        <div class="pf-item"><div class="pf-line-strong">联系人</div><div class="pf-line-muted">{{ org.contactName || "—" }}</div></div>
        <div class="pf-item"><div class="pf-line-strong">电话</div><div class="pf-line-muted">{{ org.contactPhone || "—" }}</div></div>
        <div class="pf-item"><div class="pf-line-strong">所属代理</div><div class="pf-line-muted">{{ org.agentName || "—" }}</div></div>
        <div class="pf-item"><div class="pf-line-strong">所属业务员</div><div class="pf-line-muted">{{ org.salesmanName || "—" }}</div></div>
      </template>

      <template v-else>
        <div class="pf-item"><div class="pf-line-strong">平台管理员信息</div><div class="pf-line-muted">{{ org.adminName || "平台管理员" }}</div></div>
        <div class="pf-item"><div class="pf-line-strong">平台名称</div><div class="pf-line-muted">{{ org.platformName || "环保油平台" }}</div></div>
      </template>
    </div>
  </div>
</template>

