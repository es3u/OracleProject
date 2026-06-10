package com.example.token;

import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractMediator;

public class TokenCacheMediator extends AbstractMediator {

    // نخزن التوكن هنا في الذاكرة
    private static String tokenCache;

    // نخزن وقت انتهاء التوكن هنا
    private static long tokenCacheExpiryTime;

    // يحدد العملية المطلوبة من الكلاس: CHECK أو SAVE
    private String action;

    public String getAction() {
        return action;
    }

    // MI يستدعي هذا setter من الـ XML
    public void setAction(String action) {
        this.action = action;
    }

    public boolean mediate(MessageContext context) {

        // إذا المطلوب فحص الكاش
        if ("CHECK".equalsIgnoreCase(action)) {

            // إذا التوكن موجود ولم ينتهِ
            if (tokenCache != null && System.currentTimeMillis() < tokenCacheExpiryTime) {

                // نرجع حالة HIT ونحط التوكن في context
                context.setProperty("TOKEN_CACHE_STATUS", "HIT");
                context.setProperty("ACCESS_TOKEN", tokenCache);

            } else {

                // التوكن غير موجود أو منتهي
                context.setProperty("TOKEN_CACHE_STATUS", "MISS");
                context.setProperty("ACCESS_TOKEN", null);
            }

        // إذا المطلوب حفظ توكن جديد
        } else if ("SAVE".equalsIgnoreCase(action)) {

            // نقرأ التوكن من context
            String newToken = (String) context.getProperty("ACCESS_TOKEN");

            // إذا التوكن موجود، نحفظه
            if (newToken != null && !newToken.isEmpty()) {

                tokenCache = newToken;

                // مدة الصلاحية مؤقتًا ساعة واحدة
                tokenCacheExpiryTime = System.currentTimeMillis() + (3600 * 1000L);

                context.setProperty("TOKEN_CACHE_STATUS", "SAVED");

            } else {

                // فشل الحفظ لأن التوكن غير موجود
                context.setProperty("TOKEN_CACHE_STATUS", "SAVE_FAILED");
            }

        } else {

            // action غير معروف
            context.setProperty("TOKEN_CACHE_STATUS", "INVALID_ACTION");
        }

        // true تعني كمل تنفيذ الـ API/Sequence
        return true;
    }
}