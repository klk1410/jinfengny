/** 订单 / 工单状态：0 待确认 1 待分配 2 已接收 3 已完成 4 取消 */
export function orderWorkStatusPillClass(code) {
  const map = {
    "0": "biz-pill biz-pill--warn",
    "1": "biz-pill biz-pill--info",
    "2": "biz-pill biz-pill--progress",
    "3": "biz-pill biz-pill--ok",
    "4": "biz-pill biz-pill--bad"
  };
  return map[code] ?? "biz-pill biz-pill--muted";
}

/** 店铺审核 / 提现：0 待审或待处理 1 通过 2 驳回 */
export function auditLikeStatusPillClass(code) {
  if (code === "0") {
    return "biz-pill biz-pill--warn";
  }
  if (code === "1") {
    return "biz-pill biz-pill--ok";
  }
  if (code === "2") {
    return "biz-pill biz-pill--bad";
  }
  return "biz-pill biz-pill--muted";
}

/** 商家、代理、运维：0 正常 1 停用 */
export function entityOnOffPillClass(code) {
  if (code === "0") {
    return "biz-pill biz-pill--ok";
  }
  if (code === "1") {
    return "biz-pill biz-pill--neutral";
  }
  return "biz-pill biz-pill--muted";
}

/** 合作跟进：0 跟进中 1 已签约 */
export function coopStatusPillClass(code) {
  if (code === "0") {
    return "biz-pill biz-pill--progress";
  }
  if (code === "1") {
    return "biz-pill biz-pill--ok";
  }
  return "biz-pill biz-pill--muted";
}

/** 预付款流水 directionCode：1 入账 2 支出 */
export function prepaidDirectionPillClass(code) {
  if (code === "1") {
    return "biz-pill biz-pill--ok";
  }
  if (code === "2") {
    return "biz-pill biz-pill--bad";
  }
  return "biz-pill biz-pill--muted";
}
