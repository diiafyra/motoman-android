# 0003. Fuel economy by the full-tank method

Status: accepted

## Context

The fuel used over a distance is only known when the tank is in the same state at both ends. Riders
often top up partially, forget to log a fill-up, or log one late.

## Decision

A segment runs from one full-tank fill-up (i) to the next (j). Its fuel is the sum of every fill-up
after i, up to and including j, partial fills included:

```
L/100 km = Σ(V, i+1..j) / (D_j − D_i) × 100
dong/km  = Σ(C, i+1..j) / (D_j − D_i)
```

- If any fill-up in (i, j] is marked "missed previous", the segment is dropped: some fuel was never
  logged and the figure would be too low.
- Segments with no positive distance are dropped.
- Fill-ups are ordered by time, then reading, then partial before full, then id, so the result never
  depends on database row order.
- The average over several segments is total fuel over total distance, not the mean of segment
  figures, so short segments do not weigh as much as long ones.
- With no valid segment the app shows "add one more full tank" instead of a number.

The odometer must not decrease over time; the form rejects a reading below the fill-up before it or
above the one after it.

## Alternatives

- Fuel of each fill-up divided by the distance since the previous one. Simple, but wrong for partial
  fills, which are common for motorbikes.
- Estimating the missing fuel when a fill-up was missed. Rejected: a guessed figure shown as a
  measurement is worse than no figure.

## Consequences

- Users must tick "full tank" for figures to appear; the empty state explains this.
- Some data produces no figure at all, by design.
