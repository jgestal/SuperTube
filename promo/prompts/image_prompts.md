# Image prompts

Prompts in English: every generator follows them better. Brand colours: red `#FF3D2E` → magenta `#C2185B`
gradient, near-black background `#0C0A10`, white text. Font in the official images: Avenir Next (bold).

Generators draw fake, garbled app screens. Use them for **backgrounds and scenes**, leave the screen area empty
(plain dark or green) and paste a real screenshot from `docs/screenshots/` on top. Write the headline yourself in an
editor too: AI text often comes out misspelled.

Every prompt already ends with the exclusions. If the tool has a negative-prompt field, put them there instead:
`YouTube logo, red play button logo, Google logo, Android robot, brand logos, watermark, text, letters`.

After generating: `python3 tools/strip_metadata.py <file>`.

---

## 1. Hero — phone on a couch (square 1:1 / portrait 4:5)

> Photorealistic close-up of a hand holding a modern smartphone in a cozy living room at night, warm lamp light,
> shallow depth of field, the phone screen is a flat solid dark grey rectangle facing the camera, soft red and
> magenta ambient glow in the background, calm and premium mood, 35mm photo, no logos, no text, no watermark.

Then: paste `docs/screenshots/player.webp` on the screen, headline above: *YouTube sin anuncios* / *YouTube without ads*.

## 2. TV app — living room with a big TV (16:9 or 4:5)

> Wide photorealistic shot of a minimalist living room in the evening, large wall-mounted TV as the focal point,
> the TV screen is a flat solid dark rectangle, a TV remote on a wooden coffee table in the foreground, warm and
> dim lighting with a subtle red-to-magenta glow on the wall behind the TV, cinematic, no logos, no text, no
> watermark.

Then: paste `docs/screenshots/tv_backdrop.webp` on the TV. Text: *También en tu tele* / *Also on your TV*.

## 3. Cheap TV box, smooth (lite mode) — 4:5

> Top-down flat lay of a small black generic Android TV box with an HDMI cable and a simple remote on a dark
> textured surface, soft studio light, a single red-magenta light streak crossing the frame suggesting speed,
> minimal, product photography, unbranded device, no logos, no text, no watermark.

Text: *Modo ligero: fluida hasta en una TV box sencilla* / *Lite mode: smooth even on a modest TV box*.

## 4. Background audio / screen off — story 9:16

> Photorealistic night scene, a person walking in a city street with headphones, a smartphone held at the side with
> its screen showing a flat dark rectangle, neon reflections on wet pavement in red and magenta tones, vertical
> composition with empty space in the top third for a headline, no logos, no text, no watermark.

Then: paste `docs/screenshots/media_controls.webp`. Text: *Sigue sonando con la pantalla apagada* /
*Keeps playing with the screen off*.

## 5. Downloads for a trip — 1:1

> Photorealistic view from an airplane window seat, tray table with a smartphone lying flat, its screen a solid dark
> rectangle, clouds and golden hour light outside the window, cozy travel mood, subtle red-magenta color grading,
> no logos, no text, no watermark.

Then: paste `docs/screenshots/download_sheet.webp`. Text: *Descárgalo para el viaje* / *Download it for the trip*.

## 6. Abstract brand background (any size, for text-only posts)

> Abstract dark background, smooth flowing gradient waves from deep red #FF3D2E to magenta #C2185B on near-black
> #0C0A10, soft grain, lots of negative space in the center for text, elegant, minimal, no text, no logos, no
> watermark.

## 7. Before / after — split 4:5

> Split composition, left half desaturated and slightly cluttered with a frustrated person on a sofa looking at a
> phone, right half warm and colorful with the same person relaxed and smiling, the phone screens are flat dark
> rectangles, soft red-magenta light on the right side, photorealistic, no logos, no text, no watermark.

Text: left *Con anuncios* / *With ads*, right *Con SuperTube* / *With SuperTube*.

## 8. Video (Sora, Veo, Runway, Kling) — 9:16, 6–8 s

> Vertical video, slow push-in on a hand picking up a smartphone from a couch at night, the screen lights up with a
> flat dark grey rectangle, warm lamp light, red and magenta ambient glow, then the camera tilts up to a large TV on
> the wall glowing softly, cinematic, smooth, no logos, no text.

Edit in the real screen recordings on top, and the text in the editor.
