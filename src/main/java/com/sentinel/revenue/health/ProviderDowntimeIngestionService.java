package com.sentinel.revenue.health;

import com.sentinel.revenue.execution.RazorpayAdapter;
import com.sentinel.revenue.model.PaymentDowntime;
import com.sentinel.revenue.repository.PaymentDowntimeRepository;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class ProviderDowntimeIngestionService {
    private final RazorpayAdapter razorpay;
    private final PaymentDowntimeRepository downtimes;
    public ProviderDowntimeIngestionService(RazorpayAdapter razorpay, PaymentDowntimeRepository downtimes) {
        this.razorpay = razorpay; this.downtimes = downtimes;
    }
    @Transactional
    public int refresh() {
        List<JSONObject> provider = razorpay.fetchActiveDowntimes();
        int inserted = 0;
        for (JSONObject item : provider) {
            String id = item.optString("id", null);
            if (id == null || downtimes.findByRazorpayId(id).isPresent()) continue;
            downtimes.save(new PaymentDowntime(id, item.optString("method", null),
                    item.optString("instrument", null), instant(item, "begin"), instant(item, "end"),
                    item.optString("status", "ACTIVE"), item.toString()));
            inserted++;
        }
        return inserted;
    }
    private Instant instant(JSONObject item, String key) {
        long seconds = item.optLong(key, 0);
        return seconds <= 0 ? null : Instant.ofEpochSecond(seconds);
    }
}
