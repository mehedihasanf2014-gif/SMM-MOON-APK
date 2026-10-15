package com.company.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.provider.Telephony;
import android.telephony.SmsMessage;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsReceiver extends BroadcastReceiver {
    // Firebase Authentication e ei user ta banaben (firestore.rules er email er sathe mile thakte hobe)
    static final String APP_EMAIL = "app@smmmoon.com";
    static final String APP_PASS  = "Fahad1155";

    static void signIn(Runnable then) {
        FirebaseAuth a = FirebaseAuth.getInstance();
        if (a.getCurrentUser() != null) { if (then != null) then.run(); return; }
        a.signInWithEmailAndPassword(APP_EMAIL, APP_PASS).addOnCompleteListener(t -> { if (then != null) then.run(); });
    }

    @Override public void onReceive(Context ctx, Intent i) {
        SmsMessage[] ms = Telephony.Sms.Intents.getMessagesFromIntent(i);
        if (ms == null || ms.length == 0) return;
        String from = ms[0].getOriginatingAddress();
        StringBuilder sb = new StringBuilder();
        for (SmsMessage m : ms) sb.append(m.getMessageBody());
        String body = sb.toString();

        String f = from == null ? "" : from.toLowerCase(Locale.ROOT);
        String method = f.contains("bkash") ? "bkash" : f.contains("nagad") ? "nagad" : null;
        if (method == null || !body.toLowerCase(Locale.ROOT).contains("received")) return;

        Matcher t = Pattern.compile("(?:TrxID|TxnID)\\s*[:\\-]?\\s*([A-Za-z0-9]+)", Pattern.CASE_INSENSITIVE).matcher(body);
        Matcher a = Pattern.compile("(?:Tk\\.?|Amount:?)\\s*([0-9,]+(?:\\.[0-9]+)?)", Pattern.CASE_INSENSITIVE).matcher(body);
        Matcher s = Pattern.compile("(?:from|Sender:?)\\s*(01[0-9]{9})", Pattern.CASE_INSENSITIVE).matcher(body);
        if (!t.find() || !a.find()) return;

        final String trx = t.group(1).toUpperCase(Locale.ROOT);
        final double amount = Double.parseDouble(a.group(1).replace(",", ""));
        final String sender = s.find() ? s.group(1) : "";
        final String m = method;
        final Context app = ctx.getApplicationContext();

        FirebaseApp.initializeApp(app);
        final PendingResult pr = goAsync();
        final AtomicBoolean done = new AtomicBoolean(false);
        final Runnable finish = () -> { if (done.compareAndSet(false, true)) pr.finish(); };
        new Handler(Looper.getMainLooper()).postDelayed(finish, 9000);

        signIn(() -> {
            Map<String, Object> d = new HashMap<>();
            d.put("method", m);
            d.put("trxId", trx);
            d.put("amount", amount);
            d.put("sender", sender);
            d.put("status", "unused");
            d.put("createdAt", FieldValue.serverTimestamp());
            // "transactions" collection prothom write e nijei toiri hoye jay
            FirebaseFirestore.getInstance().collection("transactions").document(m + "_" + trx)
                .set(d).addOnCompleteListener(x -> {
                    String label = (m.equals("bkash") ? "bKash " : "Nagad ") + trx + " · "
                            + new SimpleDateFormat("dd MMM HH:mm", Locale.US).format(new Date());
                    app.getSharedPreferences("smm", Context.MODE_PRIVATE).edit().putString("last", label).apply();
                    finish.run();
                });
        });
    }
}
