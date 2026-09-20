const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

// Helper to verify if the caller has active admin role in Firestore
async function verifyAdminCaller(context) {
  if (!context.auth || !context.auth.uid) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "Authentication required."
    );
  }
  const adminDoc = await db.collection("admins").doc(context.auth.uid).get();
  if (!adminDoc.exists) {
    throw new functions.https.HttpsError(
      "permission-denied",
      "User is not registered as administrator."
    );
  }
  const data = adminDoc.data();
  if (data.role !== "admin" || data.status !== "active") {
    throw new functions.https.HttpsError(
      "permission-denied",
      "Active admin credentials required."
    );
  }
  return { uid: context.auth.uid, email: data.email || context.auth.token.email || "admin" };
}

// 1. Activate or Extend Pro for a user (Server-side privileged operation)
exports.activatePro = functions.https.onCall(async (data, context) => {
  const adminInfo = await verifyAdminCaller(context);
  const { targetUid, days = 30 } = data;

  if (!targetUid) {
    throw new functions.https.HttpsError("invalid-argument", "targetUid is required.");
  }

  const now = Date.now();
  const entitlementRef = db.collection("pro_entitlements").doc(targetUid);
  const existing = await entitlementRef.get();

  let expiry = now + days * 24 * 60 * 60 * 1000;
  if (existing.exists && existing.data().isPro && existing.data().proExpiry > now) {
    // Extend existing
    expiry = existing.data().proExpiry + days * 24 * 60 * 60 * 1000;
  }

  await entitlementRef.set({
    uid: targetUid,
    isPro: true,
    proExpiry: expiry,
    updatedAt: now,
    updatedBy: adminInfo.email,
  }, { merge: true });

  // Update users collection
  await db.collection("users").doc(targetUid).set({
    isPro: true,
    proExpiry: expiry,
  }, { merge: true });

  // Audit log
  await db.collection("admin_audit_logs").add({
    adminId: adminInfo.email,
    action: "ACTIVATE_PRO",
    targetUid: targetUid,
    metadata: `Granted Pro for ${days} days until ${new Date(expiry).toISOString()}`,
    createdAt: now,
  });

  return { success: true, isPro: true, proExpiry: expiry };
});

// 2. Revoke Pro for a user
exports.revokePro = functions.https.onCall(async (data, context) => {
  const adminInfo = await verifyAdminCaller(context);
  const { targetUid } = data;

  if (!targetUid) {
    throw new functions.https.HttpsError("invalid-argument", "targetUid is required.");
  }

  const now = Date.now();
  await db.collection("pro_entitlements").doc(targetUid).set({
    uid: targetUid,
    isPro: false,
    proExpiry: 0,
    updatedAt: now,
    updatedBy: adminInfo.email,
  }, { merge: true });

  await db.collection("users").doc(targetUid).set({
    isPro: false,
    proExpiry: 0,
  }, { merge: true });

  await db.collection("admin_audit_logs").add({
    adminId: adminInfo.email,
    action: "REVOKE_PRO",
    targetUid: targetUid,
    metadata: "Revoked Pro subscription",
    createdAt: now,
  });

  return { success: true, isPro: false };
});

// 3. Approve Payment Order & Auto-Grant Pro
exports.approvePayment = functions.https.onCall(async (data, context) => {
  const adminInfo = await verifyAdminCaller(context);
  const { paymentId } = data;

  if (!paymentId) {
    throw new functions.https.HttpsError("invalid-argument", "paymentId is required.");
  }

  const paymentRef = db.collection("payment_orders").doc(paymentId);
  const paymentDoc = await paymentRef.get();
  if (!paymentDoc.exists) {
    throw new functions.https.HttpsError("not-found", "Payment order not found.");
  }

  const payment = paymentDoc.data();
  const now = Date.now();

  await paymentRef.update({
    status: "PAID",
    verifiedAt: now,
    verifiedBy: adminInfo.email,
  });

  // Automatically grant 30 days Pro
  const expiry = now + 30 * 24 * 60 * 60 * 1000;
  await db.collection("pro_entitlements").doc(payment.uid).set({
    uid: payment.uid,
    isPro: true,
    proExpiry: expiry,
    updatedAt: now,
    updatedBy: adminInfo.email,
  }, { merge: true });

  await db.collection("users").doc(payment.uid).set({
    isPro: true,
    proExpiry: expiry,
  }, { merge: true });

  // Record payment event & audit log
  await db.collection("payment_events").add({
    paymentId: paymentId,
    uid: payment.uid,
    action: "APPROVED",
    adminId: adminInfo.email,
    amount: payment.amount,
    provider: payment.provider,
    transactionReference: payment.transactionReference,
    createdAt: now,
  });

  await db.collection("admin_audit_logs").add({
    adminId: adminInfo.email,
    action: "APPROVE_PAYMENT",
    targetUid: payment.uid,
    metadata: `Approved payment ${paymentId} (${payment.provider} ৳${payment.amount}, TrxID: ${payment.transactionReference}) and granted 30 days Pro`,
    createdAt: now,
  });

  return { success: true, status: "PAID" };
});

// 4. Reject Payment Order
exports.rejectPayment = functions.https.onCall(async (data, context) => {
  const adminInfo = await verifyAdminCaller(context);
  const { paymentId, reason = "Invalid Transaction Reference" } = data;

  if (!paymentId) {
    throw new functions.https.HttpsError("invalid-argument", "paymentId is required.");
  }

  const paymentRef = db.collection("payment_orders").doc(paymentId);
  const paymentDoc = await paymentRef.get();
  if (!paymentDoc.exists) {
    throw new functions.https.HttpsError("not-found", "Payment order not found.");
  }

  const payment = paymentDoc.data();
  const now = Date.now();

  await paymentRef.update({
    status: "REJECTED",
    rejectionReason: reason,
    verifiedAt: now,
    verifiedBy: adminInfo.email,
  });

  await db.collection("payment_events").add({
    paymentId: paymentId,
    uid: payment.uid,
    action: "REJECTED",
    reason: reason,
    adminId: adminInfo.email,
    createdAt: now,
  });

  await db.collection("admin_audit_logs").add({
    adminId: adminInfo.email,
    action: "REJECT_PAYMENT",
    targetUid: payment.uid,
    metadata: `Rejected payment ${paymentId} (${payment.provider} ৳${payment.amount})`,
    createdAt: now,
  });

  return { success: true, status: "REJECTED" };
});

// 5. Update Payment Gateway Settings
exports.updatePaymentSettings = functions.https.onCall(async (data, context) => {
  const adminInfo = await verifyAdminCaller(context);
  const {
    bkashNumber,
    nagadNumber,
    bkashEnabled,
    nagadEnabled,
    monthlyPriceBdt,
    paymentInstructions,
  } = data;

  const now = Date.now();
  const config = {
    bkashNumber: bkashNumber || "01700000000",
    nagadNumber: nagadNumber || "01800000000",
    bkashEnabled: bkashEnabled !== false,
    nagadEnabled: nagadEnabled !== false,
    monthlyPriceBdt: parseInt(monthlyPriceBdt) || 150,
    paymentInstructions: paymentInstructions || "Send Money to the above number, then enter the TrxID below.",
    updatedAt: now,
    updatedBy: adminInfo.email,
  };

  await db.collection("payment_settings").doc("config").set(config, { merge: true });

  await db.collection("admin_audit_logs").add({
    adminId: adminInfo.email,
    action: "UPDATE_PAYMENT_SETTINGS",
    targetUid: "SYSTEM",
    metadata: `Updated payment settings: bKash=${config.bkashNumber} (${config.bkashEnabled}), Nagad=${config.nagadNumber} (${config.nagadEnabled}), Price=৳${config.monthlyPriceBdt}`,
    createdAt: now,
  });

  return { success: true, config };
});
