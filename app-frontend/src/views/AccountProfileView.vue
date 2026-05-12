<script setup>
import { inject, onMounted, ref, watch } from "vue";
import { requestJson } from "../api.js";
import "./promo/promo-form.css";

const shell = inject("appShell");
const err = ref("");
const base = ref({});
const org = ref({});
const sharedOpenid = ref("");
const shares = ref([]);

async function load() {
  err.value = "";
  try {
    const oid = shell.loginOpenid;
    const data = await requestJson(`/app-api/biz/account/profile?openid=${encodeURIComponent(oid)}`);
    const shareRows = await requestJson(`/app-api/biz/account/shares?openid=${encodeURIComponent(oid)}`);
    base.value = {
      roleName: data.roleName || "",
      openid: data.openid || "",
      bindTime: data.bindTime || ""
    };
    org.value = data.orgInfo || {};
    shares.value = shareRows || [];
  } catch (e) {
    err.value = e.message || String(e);
    base.value = {};
    org.value = {};
    shares.value = [];
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

async function addShare() {
  err.value = "";
  try {
    const target = sharedOpenid.value.trim();
    if (!target) {
      throw new Error("请填写共享账号 openid");
    }
    await requestJson("/app-api/biz/account/shares", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        openid: shell.loginOpenid,
        sharedOpenid: target
      })
    });
    sharedOpenid.value = "";
    await load();
  } catch (e) {
    err.value = e.message || String(e);
  }
}

async function removeShare(target) {
  if (!window.confirm(`移除共享账号 ${target}？`)) return;
  err.value = "";
  try {
    const q = new URLSearchParams();
    q.set("openid", shell.loginOpenid);
    q.set("sharedOpenid", target);
    await requestJson(`/app-api/biz/account/shares/remove?${q.toString()}`, {
      method: "POST"
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
  <div class="pf-page">
    <p v-if="err" class="pf-err">{{ err }}</p>

    <div class="pf-panel" style="margin-top: 0">
      <h3 class="pf-panel-title">基础信息卡</h3>
      <div class="dc-stack">
        <article class="dc-card dc-card--white">
          <div class="pf-line-strong">当前角色</div>
          <div class="pf-line-muted">{{ base.roleName || "—" }}</div>
        </article>
        <article class="dc-card dc-card--white">
          <div class="pf-line-strong">openid</div>
          <div class="pf-line-muted" style="word-break: break-all">{{ base.openid || "—" }}</div>
          <button type="button" class="pf-tool pf-tool--ghost" style="margin-top: 8px" @click="copyOpenid">复制</button>
        </article>
        <article class="dc-card dc-card--white">
          <div class="pf-line-strong">绑定时间（可选）</div>
          <div class="pf-line-muted">{{ base.bindTime || "—" }}</div>
        </article>
      </div>
    </div>

    <div class="pf-panel">
      <h3 class="pf-panel-title">组织信息卡</h3>

      <div v-if="base.roleName === '代理'" class="dc-stack">
        <article class="dc-card dc-card--white"><div class="pf-line-strong">代理名称</div><div class="pf-line-muted">{{ org.agentName || "—" }}</div></article>
        <article class="dc-card dc-card--white"><div class="pf-line-strong">联系人</div><div class="pf-line-muted">{{ org.contactName || "—" }}</div></article>
        <article class="dc-card dc-card--white"><div class="pf-line-strong">电话</div><div class="pf-line-muted">{{ org.contactPhone || "—" }}</div></article>
        <article class="dc-card dc-card--white"><div class="pf-line-strong">区域</div><div class="pf-line-muted">{{ org.region || "—" }}</div></article>
      </div>

      <div v-else-if="base.roleName === '业务员'" class="dc-stack">
        <article class="dc-card dc-card--white"><div class="pf-line-strong">姓名</div><div class="pf-line-muted">{{ org.salesmanName || "—" }}</div></article>
        <article class="dc-card dc-card--white"><div class="pf-line-strong">电话</div><div class="pf-line-muted">{{ org.phone || "—" }}</div></article>
        <article class="dc-card dc-card--white"><div class="pf-line-strong">所属代理</div><div class="pf-line-muted">{{ org.agentName || "—" }}</div></article>
      </div>

      <div v-else-if="base.roleName === '商家'" class="dc-stack">
        <article class="dc-card dc-card--white"><div class="pf-line-strong">门店名</div><div class="pf-line-muted">{{ org.merchantName || "—" }}</div></article>
        <article class="dc-card dc-card--white"><div class="pf-line-strong">联系人</div><div class="pf-line-muted">{{ org.contactName || "—" }}</div></article>
        <article class="dc-card dc-card--white"><div class="pf-line-strong">电话</div><div class="pf-line-muted">{{ org.contactPhone || "—" }}</div></article>
        <article class="dc-card dc-card--white"><div class="pf-line-strong">所属代理</div><div class="pf-line-muted">{{ org.agentName || "—" }}</div></article>
        <article class="dc-card dc-card--white"><div class="pf-line-strong">所属业务员</div><div class="pf-line-muted">{{ org.salesmanName || "—" }}</div></article>
      </div>

      <div v-else class="dc-stack">
        <article class="dc-card dc-card--white"><div class="pf-line-strong">平台管理员信息</div><div class="pf-line-muted">{{ org.adminName || "平台管理员" }}</div></article>
        <article class="dc-card dc-card--white"><div class="pf-line-strong">平台名称</div><div class="pf-line-muted">{{ org.platformName || "环保油平台" }}</div></article>
      </div>
    </div>

    <div class="pf-panel">
      <h3 class="pf-panel-title">共享账号</h3>
      <div class="dc-stack">
        <article class="dc-card dc-card--white">
          <div class="pf-line-strong">新增共享 openid</div>
          <div class="pf-line-muted" style="display: flex; gap: 8px; margin-top: 8px">
            <input
              v-model="sharedOpenid"
              type="text"
              placeholder="请输入对方 openid"
              style="flex: 1; padding: 8px; border-radius: 8px; border: 1px solid #d0d7e2"
            />
            <button type="button" class="pf-tool" @click="addShare">添加</button>
          </div>
        </article>
        <article class="dc-card dc-card--white">
          <div class="pf-line-strong">已共享账号</div>
          <div v-if="!shares.length" class="pf-line-muted">暂无</div>
          <div v-else class="dc-stack" style="margin-top: 10px">
            <article v-for="s in shares" :key="s.shareId" class="dc-card dc-card--white">
              <div style="display: flex; justify-content: space-between; gap: 8px; align-items: center">
                <div class="pf-line-muted" style="word-break: break-all; flex: 1; min-width: 0">
                  {{ s.sharedOpenid }} · {{ s.roleName }} · {{ s.createTime }}
                </div>
                <button type="button" class="pf-tool pf-tool--ghost" @click="removeShare(s.sharedOpenid)">移除</button>
              </div>
            </article>
          </div>
        </article>
      </div>
    </div>
  </div>
</template>

