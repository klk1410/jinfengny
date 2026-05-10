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
</script>

<template>
  <div class="page">
    <h1>环保油服务号页面（首版）</h1>

    <section class="card">
      <h2>模块一：账号信息</h2>
      <div class="row">
        <label>微信 openid</label>
        <input v-model="loginOpenid" placeholder="输入 openid" />
        <button class="btn" @click="login">登录</button>
      </div>
      <p class="tip">可测试 openid：main-openid-001 / agent-openid-001 / sales-openid-001 / merchant-openid-001</p>
      <pre v-if="userInfo">{{ JSON.stringify(userInfo, null, 2) }}</pre>
    </section>

    <section class="card">
      <h2>模块二：环保油管理入口</h2>
      <div v-if="!portal" class="warn">请先登录。</div>
      <template v-else>
        <div v-if="!canOpenBusiness" class="warn">当前账号未授权，无法打开环保油管理模块。</div>
        <template v-else>
          <div class="grid">
            <div v-for="item in portal.gridEntries" :key="item" class="grid-item">{{ item }}</div>
          </div>

          <h3>商家下单</h3>
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
          <button class="btn" @click="createOrder">提交订单</button>
        </template>
      </template>
    </section>

    <section class="card">
      <h2>模块三：账号操作</h2>
      <p>这里可继续扩展：绑定手机号、退出登录、共享账号管理、消息订阅设置。</p>
      <h3>我的订单</h3>
      <table>
        <thead>
          <tr>
            <th>订单号</th>
            <th>类型</th>
            <th>状态</th>
            <th>金额</th>
            <th>支付方式</th>
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
            <td>
              <button class="text-btn" @click="cancelOrder(item.orderNo)">取消</button>
            </td>
          </tr>
        </tbody>
      </table>
    </section>
  </div>
</template>

<style scoped>
.page {
  max-width: 980px;
  margin: 0 auto;
  padding: 16px;
  font-family: "Microsoft YaHei", Arial, sans-serif;
}

.card {
  margin-bottom: 14px;
  border: 1px solid #e7e7e7;
  border-radius: 8px;
  padding: 14px;
  background: #fff;
}

.row {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}

.tip {
  color: #666;
  font-size: 12px;
}

label {
  display: block;
  margin-bottom: 4px;
}

input,
select {
  border: 1px solid #d2d2d2;
  border-radius: 6px;
  padding: 8px;
}

.btn {
  background: #1677ff;
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 8px 14px;
  cursor: pointer;
}

.text-btn {
  border: none;
  background: transparent;
  color: #1677ff;
  cursor: pointer;
}

.warn {
  color: #bf3c3c;
  background: #ffeaea;
  border: 1px solid #ffd0d0;
  border-radius: 6px;
  padding: 10px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(120px, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}

.grid-item {
  border: 1px solid #cfe0ff;
  background: #f2f7ff;
  border-radius: 8px;
  padding: 16px 10px;
  text-align: center;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(150px, 1fr));
  gap: 10px;
  margin-bottom: 10px;
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

pre {
  background: #f7f7f7;
  border-radius: 6px;
  padding: 8px;
  overflow: auto;
}
</style>
