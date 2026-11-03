# SuperTube social media kit

Ready images: `promo/<lang>/` (en, es, fr, de, pt, it), regenerated from the real screenshots with
`python3 tools/landing/promo.py`.

| File | Use it for |
|---|---|
| `instagram_post_1080x1080.jpg` | Instagram / Facebook / Threads square post, Mastodon, Telegram |
| `instagram_features_1080x1350.jpg` | Carousel slide 2 (feature list) |
| `instagram_tv_1080x1350.jpg` | Carousel slide 3 or a post about the TV app |
| `story_1080x1920.jpg` | Instagram / Facebook stories, TikTok / Reels / Shorts cover |
| `x_header_1500x500.jpg` | X / Bluesky / Mastodon profile header |
| `og_1200x630.jpg` | Link preview (X, LinkedIn, Reddit, WhatsApp). The website already uses it |

Suggested carousel: post → features → TV.

## In this folder

- [`image_prompts.md`](image_prompts.md): prompts for image generators (GPT Image, Gemini, Midjourney, Flux…) when
  you want new artwork beyond the screenshot compositions.
- [`captions_es.md`](captions_es.md) / [`captions_en.md`](captions_en.md): ready-to-paste posts per network.
- [`llm_prompt.md`](llm_prompt.md): a prompt for ChatGPT / Claude / Gemini to write more posts or translate them
  (fr, de, pt, it) with the same facts and rules.

## Rules for every post

1. **Link to the website**, not to an APK: `https://gestal.es/supertube/`. It always serves the newest version.
2. **No YouTube logo, red play button or Google logos** in images. The name "YouTube" can appear in text to say what
   the app plays ("YouTube without ads"), never as if it were our brand.
3. **Only claims the app backs up**: free, no ads, open source (MIT), SponsorBlock, mini player / PiP / background
   audio, downloads up to 1080p, favorites in folders, optional sign-in, Android TV app with lite mode.
   Not on Google Play: say "download the APK from the website".
4. Where there is room (bio, Reddit, long posts), add: *Independent project, not affiliated with Google or YouTube.*
5. **Before uploading any new image** (generated or edited), remove its metadata:

   ```sh
   python3 tools/strip_metadata.py promo        # or the path of the new file
   ```

   It removes EXIF, XMP, comments and C2PA "content credentials" without touching the pixels. Watermarks drawn into
   the pixels by some generators (e.g. Google SynthID) are not metadata and stay.
