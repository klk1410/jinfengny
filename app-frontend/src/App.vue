<script setup>
import { computed, ref } from "vue";

const loginOpenid = ref("merchant-openid-001");
const userInfo = ref(null);
const portal = ref(null);
const myOrders = ref([]);

const orderForm = ref({
  merchantId: 10001,
  orderType: "加油",
  unitPrice: 300,
  bucketCount: 2,
  payType: "现结"
});

const canOpenBusiness = computed(() => !!portal.value?.hasBusinessAccess);
const portalSections = computed(() => portal.value?.sections || []);

async function requestJson(url, options) {
  const res = await fetch(url, options);
  const json = await res.json();
  if (json.code !== 200) {
    throw new Error(json.message || "请求失败");
  }
  return json.data;
}

async function login() {
  userInfo.value = await requestJson("/app-api/auth/wechat-login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ openid: loginOpenid.value })
  });
  portal.value = await requestJson(`/app-api/portal/modules?openid=${encodeURIComponent(loginOpenid.value)}`);
  await loadOrders();
}

async function loadOrders() {
  if (!loginOpenid.value) return;
  myOrders.value = await requestJson(`/app-api/order/list?openid=${encodeURIComponent(loginOpenid.value)}`);
}

async function createOrder() {
  await requestJson("/app-api/order/create", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      openid: loginOpenid.value,
      merchantId: Number(orderForm.value.merchantId),
      orderType: orderForm.value.orderType,
      unitPrice: Number(orderForm.value.unitPrice),
      bucketCount: Number(orderForm.value.bucketCount),
      payType: orderForm.value.payType
    })
  });
  await loadOrders();
}

async function cancelOrder(orderNo) {
  await requestJson(`/app-api/order/cancel/${orderNo}?openid=${encodeURIComponent(loginOpenid.value)}`, {
    method: "POST"
  });
  await loadOrders();
}

function onGridTap(entry) {
  const path = entry.routePath || "";
  if (path) {
    alert(`${entry.label}\n路由：${path}`);
  }
}
</script>

<template>
  <div class="shell">
    <header class="mp-header">
      <span class="back" aria-hidden="true">‹</span>
      <h1 class="mp-title">环保油管理</h1>
      <span class="mp-actions"><span class="dot">⋯</span><span class="o">⌾</span></span>
    </header>

    <div class="page">
      <section class="card">
        <h2>账号登录（测试）</h2>
        <div class="row">
          <input v-model="loginOpenid" placeholder="输入 openid" />
          <button class="btn" @click="login">刷新门户</button>
        </div>
        <p class="tip">
          示例 openid：main-openid-001 / agent-openid-001 / sales-openid-001 / merchant-openid-001；
          九宫格内容由后台数据库与角色权限驱动。
        </p>
      </section>

      <section v-if="userInfo" class="card muted-card">
        <h3>用户信息</h3>
        <pre>{{ JSON.stringify(userInfo, null, 2) }}</pre>
      </section>

      <section class="portal-wrap">
        <div v-if="!portal" class="warn-card">请先「刷新门户」加载九宫格。</div>
        <div v-else-if="!canOpenBusiness" class="warn-card">当前角色无环保油业务入口权限（guest）。</div>
        <template v-else>
          <div class="bulletin">
            📢 功能入口由后台「九宫格配置 + 角色权限」动态下发，点此格子弹出路由占位。
          </div>

          <div v-for="(sec, si) in portalSections" :key="si" class="section">
            <div class="sec-title-wrap">
              <span class="line" />
              <span class="dot">●</span>
              <span class="sec-title">{{ sec.title }}</span>
              <span class="dot">●</span>
              <span class="line" />
            </div>
            <div class="grid-box">
              <button
                v-for="(it, ii) in sec.items"
                :key="ii"
                type="button"
                class="grid-cell"
                @click="onGridTap(it)"
              >
                <div class="ico">{{ it.icon || "◇" }}</div>
                <div class="lbl">{{ it.label }}</div>
              </button>
            </div>
          </div>

          <p v-if="!portalSections.length" class="warn-card small">暂无可见功能，请到管理后台为该角色勾选权限。</p>
        </template>
      </section>

      <section class="card" v-if="portal && canOpenBusiness">
        <h2>商家下单（示例）</h2>
        <div class="form-grid">
          <div>
            <label>商家ID</label>
            <input v-model="orderForm.merchantId" />
          </div>
          <div>
            <label>订单类型</label>
            <select v-model="orderForm.orderType">
              <option>加油</option>
              <option>维护</option>
            </select>
          </div>
          <div>
            <label>单价（元/桶）</label>
            <input v-model="orderForm.unitPrice" />
          </div>
          <div>
            <label>数量（桶）</label>
            <input v-model="orderForm.bucketCount" />
          </div>
          <div>
            <label>支付方式</label>
            <select v-model="orderForm.payType">
              <option>现结</option>
              <option>赊欠</option>
            </select>
          </div>
        </div>
        <button class="btn full" @click="createOrder">提交订单</button>
      </section>

      <section class="card" v-if="loginOpenid">
        <h3>我的订单</h3>
        <table>
          <thead>
            <tr>
              <th>订单号</th>
              <th>类型</th>
              <th>状态</th>
              <th>金额</th>
              <th>支付</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in myOrders" :key="item.orderNo">
              <td>{{ item.orderNo }}</td>
              <td>{{ item.orderType }}</td>
              <td>{{ item.status }}</td>
              <td>{{ item.amountPayable }}</td>
              <td>{{ item.payType }}</td>
              <td><button class="text-btn" @click="cancelOrder(item.orderNo)">取消</button></td>
            </tr>
          </tbody>
        </table>
      </section>
    </div>
  </div>
</template>

<style scoped>
.shell {
  min-height: 100vh;
  background: #f3f5f9;
}

.mp-header {
  display: grid;
  grid-template-columns: 48px 1fr 72px;
  align-items: center;
  padding: 10px 12px calc(10px + env(safe-area-inset-top));
  background: linear-gradient(90deg, #1f6dff, #2b8cff);
  color: #fff;
  box-shadow: 0 2px 8px rgba(31, 109, 255, 0.35);
}

.back {
  font-size: 28px;
  line-height: 1;
  opacity: 0.9;
}

.mp-title {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
  text-align: center;
}

.mp-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  font-size: 14px;
  opacity: 0.9;
}

.dot {
  transform: rotate(90deg);
  display: inline-block;
}

.page {
  max-width: 640px;
  margin: 0 auto;
  padding: 12px;
  padding-bottom: 24px;
  font-family: "Microsoft YaHei", Arial, sans-serif;
}

.card {
  background: #fff;
  border-radius: 10px;
  padding: 14px;
  margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.muted-card pre {
  background: #fafafa;
  border-radius: 6px;
  padding: 8px;
  overflow: auto;
  font-size: 12px;
}

.row {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

label {
  display: block;
  margin-bottom: 4px;
  font-size: 13px;
  color: #555;
}

input,
select {
  border: 1px solid #d7dade;
  border-radius: 8px;
  padding: 8px;
  flex: 1;
  min-width: 140px;
}

.btn {
  background: #1f6dff;
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 8px 14px;
  cursor: pointer;
  font-weight: 500;
}

.btn.full {
  width: 100%;
  margin-top: 12px;
}

.tip {
  color: #666;
  font-size: 12px;
  margin: 8px 0 0;
}

.warn-card {
  background: #fff7e6;
  border: 1px solid #ffd27a;
  color: #8a5b00;
  border-radius: 10px;
  padding: 12px;
}

.warn-card.small {
  font-size: 13px;
}

.bulletin {
  margin: 4px 0 14px;
  padding: 10px 12px;
  border-radius: 10px;
  font-size: 13px;
  color: #b8860b;
  background: linear-gradient(90deg, #2b2b2b, #1a1a1a);
}

.section {
  background: #fff;
  border-radius: 12px;
  padding: 12px 10px 16px;
  margin-bottom: 12px;
  box-shadow: 0 4px 16px rgba(20, 40, 70, 0.06);
}

.sec-title-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 14px;
}

.sec-title {
  font-size: 14px;
  font-weight: 600;
  color: #24324b;
}

.line {
  flex: 1;
  border-bottom: 1px dashed #cfd7e8;
}

.grid-box {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px 8px;
}

.grid-cell {
  border: none;
  background: #f9fbff;
  border-radius: 12px;
  padding: 12px 6px;
  box-shadow: inset 0 0 0 1px #e4ecfb;
  cursor: pointer;
  text-align: center;
}

.ico {
  font-size: 28px;
  line-height: 1.2;
  margin-bottom: 6px;
}

.lbl {
  font-size: 12px;
  color: #1f2430;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  margin-bottom: 8px;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th,
td {
  border: 1px solid #ebebeb;
  padding: 8px;
  font-size: 13px;
}

.text-btn {
  border: none;
  background: transparent;
  color: #1677ff;
  cursor: pointer;
}

@media (max-width: 440px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
