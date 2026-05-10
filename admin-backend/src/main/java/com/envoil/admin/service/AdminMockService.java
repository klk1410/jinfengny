package com.envoil.admin.service;

import com.envoil.admin.model.DashboardSummary;
import com.envoil.admin.model.MerchantView;
import com.envoil.admin.model.OrderView;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminMockService {

    public DashboardSummary dashboard() {
        DashboardSummary summary = new DashboardSummary();
        summary.setAgentCount(12);
        summary.setSalesmanCount(56);
        summary.setMerchantCount(130);
        summary.setOrderPendingCount(18);
        summary.setWorkPendingCount(9);
        return summary;
    }

    public List<MerchantView> merchants() {
        MerchantView m1 = new MerchantView();
        m1.setMerchantId(10001L);
        m1.setMerchantName("粤海饭店");
        m1.setContactName("张经理");
        m1.setContactPhone("13800138000");
        m1.setAgentName("广州一代");
        m1.setSalesmanName("李师傅");
        m1.setStatus("正常");

        MerchantView m2 = new MerchantView();
        m2.setMerchantId(10002L);
        m2.setMerchantName("潮味大排档");
        m2.setContactName("王老板");
        m2.setContactPhone("13800138001");
        m2.setAgentName("广州一代");
        m2.setSalesmanName("赵师傅");
        m2.setStatus("正常");

        return Arrays.asList(m1, m2);
    }

    public List<OrderView> orders() {
        OrderView o1 = new OrderView();
        o1.setOrderNo("EO202605100001");
        o1.setMerchantName("粤海饭店");
        o1.setOrderType("加油");
        o1.setStatus("待确认");
        o1.setPayType("现结");
        o1.setAmountPayable(600.0);
        o1.setCreateTime("2026-05-10 00:30:00");

        OrderView o2 = new OrderView();
        o2.setOrderNo("EO202605100002");
        o2.setMerchantName("潮味大排档");
        o2.setOrderType("维护");
        o2.setStatus("待分配");
        o2.setPayType("赊欠");
        o2.setAmountPayable(300.0);
        o2.setCreateTime("2026-05-10 00:45:00");

        return Arrays.asList(o1, o2);
    }

    public List<String> rolePerms(String roleKey) {
        if ("env_agent".equals(roleKey)) {
            return Arrays.asList(
                    "env:merchant:list",
                    "env:merchant:query",
                    "env:merchant:add",
                    "env:merchant:edit",
                    "env:salesman:list",
                    "env:salesman:query",
                    "env:order:list",
                    "env:order:confirm",
                    "env:order:cancel",
                    "env:work:list",
                    "env:work:assign",
                    "env:work:forceAssign",
                    "env:work:finish",
                    "env:stock:list",
                    "env:stock:inbound",
                    "env:user:shareGrant",
                    "env:user:shareRevoke"
            );
        }
        return new ArrayList<>();
    }

    public List<String> shareSafePermTemplate() {
        return Arrays.asList(
                "env:merchant:list",
                "env:merchant:query",
                "env:salesman:list",
                "env:salesman:query",
                "env:order:list",
                "env:order:query",
                "env:work:list",
                "env:work:query",
                "env:work:receive",
                "env:work:finish",
                "env:stock:list",
                "env:stock:query",
                "env:account:list",
                "env:account:query"
        );
    }

    public Map<String, Object> saveSharePerms(String ownerOpenid, String shareOpenid, List<String> grantedPerms) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ownerOpenid", ownerOpenid);
        result.put("shareOpenid", shareOpenid);
        result.put("grantedPerms", grantedPerms);
        result.put("status", "saved");
        result.put("message", "已保存共享子权限，后续请落表到 biz_env_user_share_perm");
        return result;
    }
}
