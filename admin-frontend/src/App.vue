<script setup>
import { computed, onMounted, ref } from "vue";

const TOKEN_KEY = "envoil_admin_token";

const token = ref(localStorage.getItem(TOKEN_KEY) || "");
const tab = ref("portal");
const loginForm = ref({ username: "admin", password: "admin123" });
const err = ref("");
const loginLoading = ref(false);

const tree = ref([]);
const roles = ref([]);
const subjects = ref([]);
const permOptions = ref([]);

const newGroup = ref({ title: "", sortOrder: 0 });
const newFunc = ref({
  groupId: null,
  permCode: "",
  label: "",
  icon: "",
  routePath: "",
  sortOrder: 0
});

const selectedRoleId = ref(null);
const rolePermSelection = ref([]);
const subForm = ref({ openid: "", roleId: null });

const summary = ref({});
const merchants = ref([]);
const orders = ref([]);

const accTypes = ref([]);
const newAccType = ref({ typeName: "", sortOrder: 0 });

const loggedIn = computed(() => !!token.value);

function authHeaders() {
  const h = { "Content-Type": "application/json" };
  if (token.value) {
    h.Authorization = `Bearer ${token.value}`;
  }
  return h;
}

async function requestJson(url, options = {}) {
  const res = await fetch(url, {
    ...options,
    headers: { ...authHeaders(), ...(options.headers || {}) }
  });
  const json = await res.json().catch(() => ({}));
  if (res.status === 401 || json.code === 401) {
    token.value = "";
    localStorage.removeItem(TOKEN_KEY);
    throw new Error("登录已过期，请重新登录");
  }
  if (json.code !== 200) {
    throw new Error(json.message || "请求失败");
  }
  return json.data;
}

async function doLogin() {
  err.value = "";
  loginLoading.value = true;
  try {
    const res = await fetch("/prod-api/admin/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(loginForm.value)
    });
    const raw = await res.text();
    let json;
    try {
      json = raw ? JSON.parse(raw) : {};
    } catch {
      throw new Error(
        res.ok
          ? "服务器返回非 JSON，请检查 /prod-api 是否反代到 admin-backend（7266）"
          : `HTTP ${res.status}：${raw.slice(0, 120)}`
      );
    }
    if (json.code !== 200) {
      throw new Error(json.message || "登录失败");
    }
    const t = json.data && json.data.token;
    if (!t) {
      throw new Error("登录成功但未返回 token，请检查后端接口");
    }
    token.value = t;
    localStorage.setItem(TOKEN_KEY, token.value);
    try {
      await refreshAll();
    } catch (e2) {
      err.value = `已登录，但加载配置失败：${e2.message || e2}`;
    }
  } catch (e) {
    err.value = e && e.message ? e.message : String(e);
  } finally {
    loginLoading.value = false;
  }
}

function logout() {
  token.value = "";
  localStorage.removeItem(TOKEN_KEY);
}

async function refreshAll() {
  await Promise.all([loadPortalTree(), loadRoles(), loadSubjects(), loadPermOptions()]);
}

async function loadPortalTree() {
  tree.value = await requestJson("/prod-api/admin/portal/tree");
}

async function loadRoles() {
  roles.value = await requestJson("/prod-api/admin/mini/roles");
  if (!selectedRoleId.value && roles.value.length) {
    selectedRoleId.value = roles.value[0].id;
    syncRolePerms();
  }
}

async function loadSubjects() {
  subjects.value = await requestJson("/prod-api/admin/mini/subjects");
}

async function loadPermOptions() {
  permOptions.value = await requestJson("/prod-api/admin/portal/perm-options");
}

function syncRolePerms() {
  const r = roles.value.find((x) => x.id === selectedRoleId.value);
  rolePermSelection.value = r ? [...r.permCodes] : [];
}

async function saveRolePerms() {
  await requestJson(`/prod-api/admin/mini/roles/${selectedRoleId.value}/perms`, {
    method: "PUT",
    body: JSON.stringify({ permCodes: rolePermSelection.value })
  });
  await loadRoles();
  syncRolePerms();
  alert("已保存角色门户权限");
}

async function createGroup() {
  await requestJson("/prod-api/admin/portal/groups", {
    method: "POST",
    body: JSON.stringify(newGroup.value)
  });
  newGroup.value = { title: "", sortOrder: 0 };
  await loadPortalTree();
}

async function delGroup(id) {
  if (!confirm("删除分组及其下门户项？")) return;
  await requestJson(`/prod-api/admin/portal/groups/${id}`, { method: "DELETE" });
  await loadPortalTree();
}

async function createFunc() {
  await requestJson("/prod-api/admin/portal/functions", {
    method: "POST",
    body: JSON.stringify(newFunc.value)
  });
  newFunc.value.permCode = "";
  newFunc.value.label = "";
  newFunc.value.icon = "";
  newFunc.value.routePath = "";
  await loadPortalTree();
  await loadPermOptions();
}

async function delFunc(id) {
  if (!confirm("删除该功能（将移除各角色中对应权限码）？")) return;
  await requestJson(`/prod-api/admin/portal/functions/${id}`, { method: "DELETE" });
  await loadPortalTree();
  await loadPermOptions();
  await loadRoles();
  syncRolePerms();
}

async function saveSubject() {
  await requestJson("/prod-api/admin/mini/subjects", {
    method: "PUT",
    body: JSON.stringify(subForm.value)
  });
  subForm.value = { openid: "", roleId: null };
  await loadSubjects();
}

async function delSubject(openid) {
  if (!confirm("删除 openid 绑定？")) return;
  await requestJson(`/prod-api/admin/mini/subjects/${encodeURIComponent(openid)}`, {
    method: "DELETE"
  });
  await loadSubjects();
}

async function loadDashboard() {
  summary.value = await requestJson("/prod-api/admin/dashboard/summary");
  merchants.value = await requestJson("/prod-api/admin/merchant/list");
  orders.value = await requestJson("/prod-api/admin/order/list");
}

async function loadAccTypes() {
  accTypes.value = await requestJson("/prod-api/admin/biz/accessory-types");
}

async function createAccType() {
  await requestJson("/prod-api/admin/biz/accessory-types", {
    method: "POST",
    body: JSON.stringify({ typeName: newAccType.value.typeName, sortOrder: Number(newAccType.value.sortOrder) || 0 })
  });
  newAccType.value = { typeName: "", sortOrder: 0 };
  await loadAccTypes();
}

async function deleteAccType(id) {
  if (!confirm("删除该种类？（软删除，已入库记录仍关联旧种类）")) return;
  await requestJson(`/prod-api/admin/biz/accessory-types/${id}`, { method: "DELETE" });
  await loadAccTypes();
}

onMounted(() => {
  if (loggedIn.value) {
    refreshAll().catch((e) => alert(e.message));
  }
});
</script>

<template>
  <div class="page">
    <div v-if="!loggedIn" class="card login-card">
      <h1>环保油管理后台</h1>
      <p class="muted">默认账号：admin / admin123（首次启动自动创建）</p>
      <div class="form-row">
        <label>用户名</label>
        <input v-model="loginForm.username" />
      </div>
      <div class="form-row">
        <label>密码</label>
        <input v-model="loginForm.password" type="password" />
      </div>
      <p v-if="err" class="err">{{ err }}</p>
      <button type="button" class="btn" :disabled="loginLoading" @click.prevent="doLogin">
        {{ loginLoading ? "登录中…" : "登录" }}
      </button>
    </div>

    <template v-else>
      <header class="topbar">
        <h1>环保油管理后台</h1>
        <button class="btn ghost" @click="logout">退出</button>
      </header>

      <nav class="tabs">
        <button :class="{ active: tab === 'portal' }" @click="tab = 'portal'">九宫格配置</button>
        <button :class="{ active: tab === 'role' }" @click="tab = 'role'">角色权限</button>
        <button :class="{ active: tab === 'sub' }" @click="tab = 'sub'">openid 绑定</button>
        <button :class="{ active: tab === 'dash' }" @click="tab = 'dash'; loadDashboard().catch((e) => alert(e.message))">
          看板 Mock
        </button>
        <button
          :class="{ active: tab === 'acc' }"
          @click="tab = 'acc'; loadAccTypes().catch((e) => alert(e.message))"
        >
          配件种类
        </button>
      </nav>

      <section v-show="tab === 'portal'" class="card">
        <h2>分组与功能项</h2>
        <p class="muted">图标可用 emoji 或静态资源 URL；routePath 为小程序/H5 路径。</p>
        <div class="inline-form">
          <input v-model="newGroup.title" placeholder="新分组标题" />
          <input v-model.number="newGroup.sortOrder" type="number" placeholder="排序" class="w-sm" />
          <button class="btn" @click="createGroup">添加分组</button>
        </div>
        <div class="inline-form">
          <select v-model.number="newFunc.groupId" class="w-md">
            <option disabled :value="null">选择分组</option>
            <option v-for="g in tree" :key="g.id" :value="g.id">{{ g.title }}</option>
          </select>
          <input v-model="newFunc.permCode" placeholder="permCode 唯一" />
          <input v-model="newFunc.label" placeholder="显示名称" />
          <input v-model="newFunc.icon" placeholder="图标" />
          <input v-model="newFunc.routePath" placeholder="路由" />
          <input v-model.number="newFunc.sortOrder" type="number" placeholder="排序" class="w-sm" />
          <button class="btn" @click="createFunc">添加功能</button>
        </div>

        <div v-for="g in tree" :key="g.id" class="group-block">
          <div class="group-head">
            <strong>{{ g.title }}</strong>
            <span class="muted">sort={{ g.sortOrder }} id={{ g.id }}</span>
            <button class="text-btn danger" @click="delGroup(g.id)">删分组</button>
          </div>
          <table>
            <thead>
              <tr>
                <th>名称</th>
                <th>perm</th>
                <th>图标</th>
                <th>路由</th>
                <th />
              </tr>
            </thead>
            <tbody>
              <tr v-for="f in g.functions" :key="f.id">
                <td>{{ f.label }}</td>
                <td class="mono">{{ f.permCode }}</td>
                <td>{{ f.icon }}</td>
                <td class="mono">{{ f.routePath }}</td>
                <td><button class="text-btn danger" @click="delFunc(f.id)">删</button></td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section v-show="tab === 'role'" class="card">
        <h2>角色门户权限</h2>
        <div class="inline-form">
          <label>角色</label>
          <select v-model.number="selectedRoleId" @change="syncRolePerms">
            <option v-for="r in roles" :key="r.id" :value="r.id">{{ r.roleName }} ({{ r.roleCode }})</option>
          </select>
          <button class="btn" @click="saveRolePerms">保存勾选</button>
        </div>
        <div class="perm-grid">
          <label v-for="p in permOptions" :key="p.permCode" class="perm-item">
            <input type="checkbox" v-model="rolePermSelection" :value="p.permCode" />
            <span class="perm-cn">{{ p.groupTitle }} · {{ p.label }}</span>
          </label>
        </div>
      </section>

      <section v-show="tab === 'sub'" class="card">
        <h2>小程序 openid → 角色</h2>
        <p class="muted">未绑定时按 openid 关键字推断（与首版 mock 一致），绑定后优先生效。</p>
        <div class="inline-form">
          <input v-model="subForm.openid" placeholder="openid" />
          <select v-model.number="subForm.roleId" class="w-md">
            <option disabled :value="null">选择角色</option>
            <option v-for="r in roles" :key="r.id" :value="r.id">{{ r.roleName }}</option>
          </select>
          <button class="btn" @click="saveSubject">保存绑定</button>
        </div>
        <table>
          <thead>
            <tr>
              <th>openid</th>
              <th>角色</th>
              <th />
            </tr>
          </thead>
          <tbody>
            <tr v-for="s in subjects" :key="s.openid">
              <td class="mono">{{ s.openid }}</td>
              <td>{{ s.roleName }}</td>
              <td><button class="text-btn danger" @click="delSubject(s.openid)">删</button></td>
            </tr>
          </tbody>
        </table>
      </section>

      <section v-show="tab === 'acc'" class="card">
        <h2>配件种类</h2>
        <p class="muted">小程序入库时下拉选项来源于此表。</p>
        <div class="inline-form">
          <input v-model="newAccType.typeName" placeholder="种类名称" />
          <input v-model.number="newAccType.sortOrder" type="number" placeholder="排序" class="w-sm" />
          <button class="btn" @click="createAccType().catch((e) => alert(e.message))">添加</button>
        </div>
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>名称</th>
              <th>排序</th>
              <th />
            </tr>
          </thead>
          <tbody>
            <tr v-for="t in accTypes" :key="t.typeId">
              <td>{{ t.typeId }}</td>
              <td>{{ t.typeName }}</td>
              <td>{{ t.sortOrder }}</td>
              <td><button class="text-btn danger" @click="deleteAccType(t.typeId)">删</button></td>
            </tr>
          </tbody>
        </table>
      </section>

      <section v-show="tab === 'dash'" class="card">
        <h2>看板 Mock</h2>
        <div class="kpi-grid">
          <div class="kpi">代理: {{ summary.agentCount || 0 }}</div>
          <div class="kpi">业务员: {{ summary.salesmanCount || 0 }}</div>
          <div class="kpi">商家: {{ summary.merchantCount || 0 }}</div>
          <div class="kpi">待确认订单: {{ summary.orderPendingCount || 0 }}</div>
        </div>
        <h3>商家</h3>
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>名称</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in merchants" :key="item.merchantId">
              <td>{{ item.merchantId }}</td>
              <td>{{ item.merchantName }}</td>
            </tr>
          </tbody>
        </table>
      </section>
    </template>
  </div>
</template>

<style scoped>
.page {
  max-width: 1180px;
  margin: 0 auto;
  padding: 24px;
  font-family: "Microsoft YaHei", Arial, sans-serif;
  color: #222;
}

.topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.tabs button {
  border: 1px solid #d0d8e8;
  background: #f5f8ff;
  padding: 8px 12px;
  border-radius: 6px;
  cursor: pointer;
}

.tabs button.active {
  background: #2376ff;
  color: #fff;
  border-color: #2376ff;
}

.card {
  background: #fff;
  border: 1px solid #e9e9e9;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
}

.login-card {
  max-width: 420px;
}

.form-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 10px;
}

input,
select,
textarea {
  border: 1px solid #cfcfcf;
  border-radius: 6px;
  padding: 8px 10px;
  font-size: 14px;
}

.btn {
  background: #2376ff;
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 8px 14px;
  cursor: pointer;
}

.btn.ghost {
  background: transparent;
  color: #2376ff;
  border: 1px solid #2376ff;
}

.muted {
  font-size: 12px;
  color: #666;
}

.err {
  color: #c0392b;
  font-size: 13px;
}

.inline-form {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-bottom: 12px;
}

.w-sm {
  width: 88px;
}
.w-md {
  min-width: 160px;
}

.group-block {
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 10px;
  margin-bottom: 12px;
}

.group-head {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 8px;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th,
td {
  border: 1px solid #ececec;
  padding: 8px;
  text-align: left;
  font-size: 13px;
}

.mono {
  font-family: Consolas, monospace;
  font-size: 12px;
}

.perm-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 8px;
  margin-top: 10px;
}

.perm-item {
  display: flex;
  gap: 6px;
  align-items: center;
}

.perm-cn {
  flex: 1;
  font-size: 13px;
  color: #222;
  line-height: 1.35;
}

.text-btn {
  border: none;
  background: transparent;
  color: #2376ff;
  cursor: pointer;
}

.text-btn.danger {
  color: #c0392b;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(120px, 1fr));
  gap: 12px;
}

.kpi {
  background: #f5f8ff;
  border: 1px solid #d6e2ff;
  border-radius: 6px;
  padding: 12px;
  text-align: center;
}

h2 {
  margin-top: 0;
}
</style>
