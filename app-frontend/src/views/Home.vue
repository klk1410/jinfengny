<script setup>
import { inject } from "vue";
import { useRouter } from "vue-router";

const router = useRouter();
const shell = inject("appShell");

function onGridTap(entry) {
  const path = entry.routePath || "";
  if (path === "#/blank" || path.startsWith("#/blank?")) {
    const qs = path.includes("?") ? path.slice(path.indexOf("?")) : "";
    const u = new URLSearchParams(qs || "");
    const q = {};
    u.forEach((val, key) => {
      q[key] = val;
    });
    if (entry.label && !q.t) {
      q.t = entry.label;
    }
    router.push({ path: "/blank", query: q });
    return;
  }
  if (path.startsWith("#/")) {
    router.push(path.slice(1));
    return;
  }
  if (path.startsWith("/")) {
    router.push(path);
    return;
  }
  if (path) {
    window.alert(`${entry.label}\n${path}`);
  } else {
    window.alert(entry.label);
  }
}
</script>

<template>
  <div>
    <template v-if="shell.portal && shell.canOpenBusiness">
      <section v-for="(sec, si) in shell.portalSections" :key="si" class="panel">
        <div class="sec-head">
          <span class="sec-line" />
          <span class="sec-dot" />
          <h2 class="sec-title">{{ sec.title }}</h2>
          <span class="sec-dot" />
          <span class="sec-line" />
        </div>
        <div class="grid">
          <button
            v-for="(it, ii) in sec.items"
            :key="ii"
            type="button"
            class="cell"
            @click="onGridTap(it)"
          >
            <div class="cell-ico">{{ it.icon || "◇" }}</div>
            <div class="cell-lbl">{{ it.label }}</div>
          </button>
        </div>
      </section>

      <p v-if="!shell.portalSections.length" class="empty">暂无可见功能，请在管理后台为该角色配置九宫格权限。</p>
    </template>

    <div v-else-if="shell.portal && !shell.canOpenBusiness" class="banner-warn">
      当前账号无业务入口权限，请联系管理员分配角色或绑定 openid。
    </div>

    <div v-else-if="!shell.loading" class="banner-warn">未能加载门户，请检查网络或后端服务。</div>
  </div>
</template>

<style scoped>
.banner-warn {
  margin: 10px 0 0;
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 13px;
  background: #fffbeb;
  color: #92400e;
  border: 1px solid #fde68a;
}

.panel {
  background: #fff;
  border-radius: 0;
  margin-bottom: 10px;
  padding: 14px 12px 16px;
  box-shadow: 0 1px 0 rgba(0, 0, 0, 0.04);
}

.sec-head {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-bottom: 16px;
}

.sec-line {
  flex: 1;
  max-width: 72px;
  height: 2px;
  background: linear-gradient(90deg, transparent, #3b82f6 40%, #3b82f6);
  border-radius: 1px;
}

.sec-head .sec-line:last-of-type {
  background: linear-gradient(90deg, #3b82f6, #3b82f6 60%, transparent);
}

.sec-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #3b82f6;
  flex-shrink: 0;
}

.sec-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
  padding: 0 4px;
  white-space: nowrap;
}

.grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px 10px;
}

.cell {
  border: none;
  background: transparent;
  padding: 4px 2px;
  cursor: pointer;
  text-align: center;
}

.cell-ico {
  width: 100%;
  aspect-ratio: 1;
  max-height: 56px;
  margin: 0 auto 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30px;
  line-height: 1;
  background: #f8fafc;
  border-radius: 12px;
  box-shadow: inset 0 0 0 1px #e8eef8;
}

.cell-lbl {
  font-size: 12px;
  color: #1e293b;
  line-height: 1.35;
}

.empty {
  text-align: center;
  color: #64748b;
  font-size: 13px;
  padding: 24px 12px;
}
</style>
