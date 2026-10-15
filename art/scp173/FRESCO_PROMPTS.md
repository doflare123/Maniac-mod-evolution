# SCP-173 fresco generation

Generated with the built-in image_gen tool as two independent compositions.

- Selection card: `src/main/resources/assets/maniacrev/textures/gui/frescos/scp_173_card.png`, 2170 x 725 (approximately 3:1).
- Character backdrop: `src/main/resources/assets/maniacrev/textures/gui/frescos/scp_173_portrait.png`, 1024 x 1536 (2:3).

Appearance references: `art/scp173/preview.png` (actual model geometry and texture) and `art/scp173/texture_source.png` (face markings and concrete). Style reference: `src/main/resources/assets/maniacrev/textures/gui/frescos/keeper_of_nightmares.png`.

The selection screen uses the card texture for both regular and large cards, and the portrait behind the character model. Both fit fully within their destination rectangles while preserving aspect ratio; unused space has a dark background. Dimensions in CharacterSelectionScreen must match the saved PNG headers. Warden artwork and other characters keep their current behavior.

## Card prompt

Use case: stylized-concept. Generate ONE new standalone horizontal panoramic 3:1 mosaic fresco texture (target 2172x724) for Minecraft mod Maniac Revolution SCP-173 selection card. Three input images have distinct roles: image 1 model appearance reference ONLY, shows three views of one statue, never replicate that contact sheet or any text; image 2 precise face paint and cracked beige concrete reference ONLY; image 3 decorative mosaic style reference ONLY. Respect model: slender cubic humanoid concrete statue, disproportionately large elongated stepped block head, narrow neck and trunk, short bent arms raised near shoulders and long thin straight legs. Face carries red vertical smeared abstract paint, narrow elongated black central mark, two upper olive green circular patches, two lower black round patches, small black oval lower mark; painted static face, not glowing creature eyes. New wide composition: single dominant SCP-173 three-quarter statue center, head and compact upper body large and legible at thumbnail size, its face directed toward viewer. On left a monumental decorative open eye medallion and tiny blocky survivor watcher looking straight at statue, thin ivory sight-line tesserae hold the scene geometrically still. Right side transitions into deep charcoal-black darkness beneath an extinguishing geometric ceiling lamp, a closed eye motif and sharp staggered beige zigzag mosaic movement trail toward a second tiny turned-away survivor (NOT a second statue), implying instant movement during a blink. Beautiful rich sinister ceremonial wall mosaic with tiny irregular square stone tesserae and black grout, ornate narrow rectangular ruby red border, red diamonds and pronounced scarlet cracks. Palette luminous dirty beige cracked concrete, bone ivory, olive face accents, muted umber, dark charcoal; ruby red around 15 percent. Match lavish craft and Minecraft cubic forms of reference 3, no photoreal, no smooth 3D. Keep main statue and essential motifs inside central 80% of canvas. Dark opaque edge-to-edge background, no external mockup. No text, no letters, no numbers, no labels, no logos, no gore. Different composition from future portrait; do not make contact sheet or collage.

## Portrait prompt

undefined

