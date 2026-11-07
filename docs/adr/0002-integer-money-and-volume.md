# 0002. Money and volume stored as integers

Status: accepted

## Context

A fill-up has a volume (litres, usually with decimals) and a cost (Vietnamese dong). The overview and
statistics add up hundreds of fill-ups. `Double` cannot represent most decimal values exactly: 0.1 L
is stored as 0.1000000000000000055…, and errors grow as values are summed. A total that shows
"1,234,999 ₫" instead of "1,235,000 ₫" makes the whole app look wrong.

## Decision

- Volume is stored in millilitres as `Long`.
- Money is stored in dong as `Long`. The dong has no subunit in practice.
- Odometer readings are whole kilometres as `Int`, capped at 9,999,999 (seven digits) so that a
  reading plus an interval cannot overflow.
- Values are converted to decimals only when shown, and derived figures such as L/100 km are computed
  as `Double` from the exact integer sums at that point.

## Alternatives

- `Double` everywhere. Rejected for the summing error above.
- `BigDecimal`. Exact, but heavier, has no Room support without a converter, and gives nothing over
  integers for values with a fixed smallest unit.

## Consequences

- Sums are exact: a test adds 1,000 fills of 0.1 L and expects exactly 100 L.
- Input parsing converts "4,25" or "4.25" litres to 4250 mL once, in the form.
- Supporting a currency with cents later would need a minor-unit field. Out of scope (see the strategy:
  no multi-currency).
