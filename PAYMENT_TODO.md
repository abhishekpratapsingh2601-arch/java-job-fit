# JavaJobFit Payment TODO

Do not implement real payments until beta demand is validated.

## India

- Provider: Razorpay
- Offer: ₹89 one-time Pro Report

## International

- Provider: Stripe
- Offer: $9.99 one-time Pro Report

## Rules

- Do not hard-code keys.
- Do not fake successful payments.
- Do not expose secrets.
- Use environment variables only.
- Paid unlock should happen only after payment confirmation from a backend webhook.
- Free API responses must not include premium hidden content.

## Future Environment Variables

```text
PAYMENT_PROVIDER_ENABLED=false
RAZORPAY_KEY_ID=
RAZORPAY_KEY_SECRET=
STRIPE_SECRET_KEY=
STRIPE_WEBHOOK_SECRET=
```
