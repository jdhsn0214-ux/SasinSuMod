# Four Divine Beasts — Skill Remake V2

Target: Minecraft 1.20.1 / Forge 47.2.0 / Java 17

This document defines the gameplay and presentation target for the V2 skill remake. It is a design target, not a claim that these features are already implemented.

## Input contract

- Right click: Skill I (basic)
- Sneak + left click: Skill II (power skill)
- Sneak + right click: Skill III (ultimate)
- Double-tap sneak: Skill IV (special)
- Keep all four weapons usable in single-player and multiplayer.
- Do not summon actual Divine Beast mobs.

## Weapon identities and skill roster

### Azure Dragon Sword — mobility, wind, storm, and flowing combos
1. **Azure Fang** — a fast forward crescent slash; a narrow travelling wave hits enemies along the blade path.
2. **Dragon Current** — a multi-stage advancing cyclone; enemies are pulled inward before a final upward launch.
3. **Heaven-Scouring Dragon** (ultimate) — brief cinematic wind-up, a huge spectral dragon-shaped energy trail crosses the battlefield, then a storm column detonates at the target zone.
4. **Cloudstep** (special) — short directional blink with a circular afterimage and a follow-up cross slash.

### White Tiger Axe — impact, pursuit, armor break, and brutal melee
1. **Rending Claw** — three visible claw-shaped shock slashes with a heavy final hit.
2. **Predator's Pursuit** — lock onto the nearest valid target in front, rush through it, then land a ground-breaking axe strike.
3. **Whiteout Execution** (ultimate) — dramatic overhead axe wind-up, expanding ground cracks/shock rings, and a large finishing impact.
4. **King's Roar** (special) — radial roar pulse that staggers nearby enemies and briefly empowers the next axe hit.

### Vermilion Dagger — speed, fire, aerial movement, and burn
1. **Ember Waltz** — quick three-hit dash combo leaving curved flame trails.
2. **Rising Phoenix** — rising multi-hit flame spiral followed by a downward burst.
3. **Solar Rebirth** (ultimate) — fiery aerial ascent, wing-shaped arcs of flame, then a large sunburst at the chosen landing point.
4. **Cinderstep** (special) — two rapid short dashes with ember afterimages; the second dash leaves an igniting line.

### Black Tortoise Spear — control, defense, ice, and counterattacks
1. **Frostline Thrust** — long piercing thrust that sends a visible ice lance forward and slows targets.
2. **Tidal Bastion** — a curved defensive barrier; absorbs/reduces incoming damage briefly, then releases a freezing counterwave.
3. **Abyssal Citadel** (ultimate) — layered ice-water rings rise around the target area, enemies are trapped/slowed, then a towering central pillar erupts.
4. **Immutable Counter** (special) — a timed guard window; a successful hit triggers a strong spear counter and a protective pulse.

## Shared presentation requirements

- Every skill has a readable start, travel/attack phase, impact, and recovery; avoid resolving every effect in one tick.
- Use weapon-specific color palettes, layered particle shapes, sound cues, and distinct hit reactions.
- Ultimate presentation combines a client-side full-screen overlay with a world-space weapon/character-centered sequence. The overlay must not obscure normal HUD permanently and must always time out safely.
- Multiplayer gameplay and damage are authoritative on the server; client visuals are triggered by small network packets.
- Add cooldown feedback and clear failure feedback for unavailable skills.
- Do not rely on hundreds of particles per tick. Use bounded particle budgets, distance-aware rendering, and short-lived effects suitable for mid-range PCs.
- Keep a configuration-friendly path for particle quality (low/medium/high) if feasible.

## Implementation order

1. Audit and stabilize current input/network handling and compile errors.
2. Add a reusable timed skill-sequence system with server-authoritative hit windows.
3. Rebuild all 16 skills using the roster above.
4. Add ultimate visual packets and client overlay/afterimage effects.
5. Add custom weapon item models and attack animation assets.
6. Run Forge build, inspect logs, then test in a real 1.20.1 client and a multiplayer session.

## Acceptance checklist

- [ ] All four weapons register and appear in the combat creative tab.
- [ ] Each weapon has four distinct skills and the documented inputs.
- [ ] Skills have multi-stage timing and distinct impact feedback.
- [ ] Ultimates show both client presentation and server-side gameplay effects.
- [ ] No Divine Beast mobs are summoned.
- [ ] No excessive particle spam or permanent HUD overlay.
- [ ] Forge 1.20.1 build passes.
- [ ] Single-player and multiplayer behavior tested.
