# JavaJobFit Payment TODO

Do not implement real payments until beta demand is validated.

## Pricing decision (final, 2026-08-07)

- **Launch price: ₹89 one-time Pro Report**, displayed anchored against a
  struck-through **₹299** regular price ("launch offer").
- Rationale: ₹89 sits inside the UPI impulse zone; an unreviewed new site
  cannot clear the trust bar at ₹299. Month-one goal is buyers/testimonials,
  not margin. Revisit (₹199 → ₹299) only after ~20-30 paid users and visible
  testimonials.
- Implementation rule: price and anchor price live in ONE config/env value
  each (e.g. PRO_PRICE_INR=89, PRO_ANCHOR_PRICE_INR=299) — never hardcoded
  in copy across files. Repricing must be a 1-line change.

## India

- Provider: Razorpay
- Offer: ₹89 one-time Pro Report (anchor ₹299 — see pricing decision above)

## International

- Provider: Stripe
- Offer: $9.99 one-time Pro Report
- Deferred: Razorpay-only at launch; add international once India converts.

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
PRO_PRICE_INR=89
PRO_ANCHOR_PRICE_INR=299
RAZORPAY_KEY_ID=
RAZORPAY_KEY_SECRET=
STRIPE_SECRET_KEY=
STRIPE_WEBHOOK_SECRET=
```
