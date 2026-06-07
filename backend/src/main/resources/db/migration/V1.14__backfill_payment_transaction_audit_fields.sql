UPDATE payment_transactions
SET paid_at = CASE WHEN status = 'PAID' THEN updated_at ELSE paid_at END,
    business_date = DATE(COALESCE(
        CASE WHEN status = 'PAID' THEN updated_at ELSE NULL END,
        created_at,
        CURRENT_TIMESTAMP
    )),
    external_reference = COALESCE(external_reference, payos_reference),
    failure_reason = COALESCE(failure_reason, cancel_reason)
WHERE business_date IS NULL
   OR external_reference IS NULL
   OR failure_reason IS NULL
   OR paid_at IS NULL;
