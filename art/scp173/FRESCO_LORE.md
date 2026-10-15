# SCP-173 containment fresco revision

Generated with the built-in image_gen tool. Two new independent compositions replace the earlier symbolic-eye artwork in the selection screen. Previous PNGs remain available under their original filenames.

## Lore source

"SCP-173" by Moto42, SCP Wiki: https://scp-wiki.wikidot.com/scp-173 (source credited under CC BY-SA; https://creativecommons.org/licenses/by-sa/3.0/).

The original article establishes locked containment, three entering personnel, two maintaining direct observation, coordinated blinking, concrete and rebar construction, painted markings, scraping sounds and dangerous cleaning duty. These provide the visual story. Failing lighting is a reference to the mod's ability, rather than a claim that the original article grants SCP-173 control over electricity.

## Visual direction

The new images depict an oppressive industrial containment chamber in finely tiled Minecraft mosaic, with strong beige concrete, charcoal recesses, olive face markings and vivid crimson emergency illumination. Industrial containment arrows and warning chevrons replace giant eye medallions and ritual ornaments. No lettering is painted into either image.

Appearance references are the actual model preview (`art/scp173/preview.png`) and face/concrete reference (`art/scp173/texture_source.png`). The keeper fresco supplies the small stone tesserae, black grout and ornate red accents.

## Prompt set

Exact successful-generation prompts are recorded in `FRESCO_CARD_LORE_PROMPT.txt` and `FRESCO_PORTRAIT_LORE_PROMPT.txt`. The card uses the generated portrait as a series reference; the portrait uses the model preview, face texture source and keeper fresco.

Card: an original panoramic 3:1 mosaic. One imposing SCP-173 at center-right, two orange-clad personnel observing from the left, a third cleanup technician near the threshold, reinforced concrete, locked steel containment door, observation window, cold strip lighting, red emergency lamps, scraped floor and darkness. Preserve the model's elongated stepped head, slender body, short bent arms and exact red/black/olive painted face. Use narrow crimson containment-arrow and industrial-chevron ornament. No giant symbolic eyes, occult motifs, magic, lettering or gore.

Portrait: an independent full-body 2:3 composition of the same statue suddenly close to a tense observer at the bottom. Show a second observer at the threshold, massive containment door frame, steel hinges, failed fluorescent panels, red emergency light, looming hard-edged shadow, floor scuffs and discreet cleaning equipment. Keep complete anatomy within the frame and breathing space around the figure for the model overlay. Match the card's cracked concrete, square mosaic tiles, black grout and crimson industrial border. No giant eyes, ritual symbols, magical beams, text or gore.

## Integration

- Card: `src/main/resources/assets/maniacrev/textures/gui/frescos/scp_173_card_lore.png`, 2170 x 725 (approximately 3:1).
- Character backdrop: `src/main/resources/assets/maniacrev/textures/gui/frescos/scp_173_portrait_lore.png`, 1024 x 1536 (2:3).

The selection screen fits each complete image without stretching or cropping. Its source dimensions must match the PNG headers. Both normal and large cards use the card artwork, while the model panel uses the portrait.
