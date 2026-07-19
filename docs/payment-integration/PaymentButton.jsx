import { useState } from "react";
import { createPaymentOrder, verifyPayment } from "./paymentApi";

function loadRazorpayCheckout() {
  return new Promise((resolve) => {
    if (window.Razorpay) {
      resolve(true);
      return;
    }
    const script = document.createElement("script");
    script.src = "https://checkout.razorpay.com/v1/checkout.js";
    script.onload = () => resolve(true);
    script.onerror = () => resolve(false);
    document.body.appendChild(script);
  });
}

function newIdempotencyKey(bookingId) {
  return `booking-${bookingId}-${crypto.randomUUID()}`;
}

export default function PaymentButton({ bookingId, customer, onSuccess }) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [attemptKey, setAttemptKey] = useState(() => newIdempotencyKey(bookingId));

  async function pay() {
    setLoading(true);
    setError("");
    try {
      if (!(await loadRazorpayCheckout())) {
        throw new Error("Unable to load Razorpay checkout");
      }
      const { data: orderEnvelope } = await createPaymentOrder(bookingId, attemptKey);
      const order = orderEnvelope.data;
      const checkout = new window.Razorpay({
        key: order.keyId,
        order_id: order.razorpayOrderId,
        amount: order.amountInPaise,
        currency: order.currency,
        name: "Lexho",
        description: `Service booking #${bookingId}`,
        prefill: {
          name: customer?.name ?? "",
          email: customer?.email ?? "",
          contact: customer?.mobile ?? "",
        },
        handler: async (response) => {
          try {
            const { data: verifiedEnvelope } = await verifyPayment({
              razorpayOrderId: response.razorpay_order_id,
              razorpayPaymentId: response.razorpay_payment_id,
              razorpaySignature: response.razorpay_signature,
            });
            if (!verifiedEnvelope.success) {
              throw new Error(verifiedEnvelope.message);
            }
            onSuccess?.(verifiedEnvelope.data);
          } catch (verificationError) {
            setError(
              verificationError.response?.data?.message ??
                verificationError.message ??
                "Unable to verify payment. Please check payment status before retrying.",
            );
          } finally {
            setLoading(false);
          }
        },
        modal: {
          ondismiss: () => setLoading(false),
        },
      });
      checkout.on("payment.failed", (response) => {
        setError(response.error?.description ?? "Payment failed. Please retry.");
        setAttemptKey(newIdempotencyKey(bookingId));
        setLoading(false);
      });
      checkout.open();
    } catch (paymentError) {
      setError(paymentError.response?.data?.message ?? paymentError.message);
      setLoading(false);
    }
  }

  return (
    <div>
      <button type="button" disabled={loading} onClick={pay}>
        {loading ? "Opening payment..." : "Pay securely"}
      </button>
      {error && (
        <div>
          <p>{error}</p>
          <button type="button" disabled={loading} onClick={pay}>
            Retry payment
          </button>
        </div>
      )}
    </div>
  );
}
