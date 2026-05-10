<script setup>
import { onMounted, ref } from "vue";

const summary = ref({});
const merchants = ref([]);
const orders = ref([]);
const safePerms = ref([]);
const shareResult = ref(null);

const form = ref({
  ownerOpenid: "agent-openid-001",
  shareOpenid: "agent-share-openid-001",
  grantedPerms: ["env:order:list", "env:work:finish"]
});

async function requestJson(url, options) {
  const res = await fetch(url, options);
  const json = await res.json();
  if (json.code !== 200) {
    throw new Error(json.message || "请求失败");
  }
  return json.data;
}

async function loadData() {
  summary.value = await requestJson("/prod-api/admin/dashboard/summary");
  merchants.value = await requestJson("/prod-api/admin/merchant/list");
  orders.value = await requestJson("/prod-api/admin/order/list");
  const shareTemplate = await requestJson("/prod-api/admin/permission/share-template");
  safePerms.value = shareTemplate.safePerms || [];
}

async function submitSharePerms() {
  shareResult.value = null;
  const data = await requestJson("/prod-api/admin/permission/share-grant", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(form.value)
  });
  shareResult.value = data;
}

onMounted(() => {
  loadData().catch((err) => {
    alert(err.message);
  });
});
</script>

<template>
  <div class="page">
    <h1>环保油管理后台（首版）</h1>

    <section class="card">
      <h2>看板</h2>
      <div class="kpi-grid">
        <div class="kpi">代理: {{ summary.agentCount || 0 }}</div>
        <div class="kpi">业务员: {{ summary.salesmanCount || 0 }}</div>
        <div class="kpi">商家: {{ summary.merchantCount || 0 }}</div>
        <div class="kpi">待确认订单: {{ summary.orderPendingCount || 0 }}</div>
        <div class="kpi">待处理工单: {{ summary.workPendingCount || 0 }}</div>
      </div>
    </section>

    <section class="card">
      <h2>共享子权限授权（白名单）</h2>
      <p class="muted">仅允许授权安全权限，不允许删除/强制指派/授权等高风险权限。</p>
      <div class="form-row">
        <label>主账号 openid</label>
        <input v-model="form.ownerOpenid" />
      </div>
      <div class="form-row">
        <label>共享账号 openid</label>
        <input v-model="form.shareOpenid" />
      </div>
      <div class="form-row">
        <label>授权权限（逗号分隔）</label>
        <textarea
          :value="form.grantedPerms.join(',')"
          @input="form.grantedPerms = $event.target.value.split(',').map(v => v.trim()).filter(Boolean)"
          rows="3"
        />
      </div>
      <button class="btn" @click="submitSharePerms">保存共享权限</button>
      <p class="muted">安全权限模板：{{ safePerms.join(" , ") }}</p>
      <pre v-if="shareResult">{{ JSON.stringify(shareResult, null, 2) }}</pre>
    </section>

    <section class="card">
      <h2>商家列表（示例）</h2>
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>商家</th>
            <th>联系人</th>
            <th>电话</th>
            <th>代理</th>
            <th>业务员</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in merchants" :key="item.merchantId">
            <td>{{ item.merchantId }}</td>
            <td>{{ item.merchantName }}</td>
            <td>{{ item.contactName }}</td>
            <td>{{ item.contactPhone }}</td>
            <td>{{ item.agentName }}</td>
            <td>{{ item.salesmanName }}</td>
          </tr>
        </tbody>
      </table>
    </section>

    <section class="card">
      <h2>订单列表（示例）</h2>
      <table>
        <thead>
          <tr>
            <th>订单号</th>
            <th>商家</th>
            <th>类型</th>
            <th>状态</th>
            <th>支付方式</th>
            <th>金额</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in orders" :key="item.orderNo">
            <td>{{ item.orderNo }}</td>
            <td>{{ item.merchantName }}</td>
            <td>{{ item.orderType }}</td>
            <td>{{ item.status }}</td>
            <td>{{ item.payType }}</td>
            <td>{{ item.amountPayable }}</td>
          </tr>
        </tbody>
      </table>
    </section>
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

h1 {
  margin-bottom: 16px;
}

.card {
  background: #fff;
  border: 1px solid #e9e9e9;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(120px, 1fr));
  gap: 12px;
}

.kpi {
  background: #f5f8ff;
  border: 1px solid #d6e2ff;
  border-radius: 6px;
  padding: 12px;
  text-align: center;
}

.form-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 10px;
}

input,
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

.muted {
  font-size: 12px;
  color: #666;
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

pre {
  margin-top: 10px;
  background: #f7f7f7;
  border-radius: 6px;
  padding: 10px;
  font-size: 12px;
}
</style>
