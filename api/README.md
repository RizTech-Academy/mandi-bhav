# The Mandi Bhav prices API

A static JSON API: plain files in this folder, served by GitHub from

```text
https://raw.githubusercontent.com/RizTech-Academy/mandi-bhav/main/api/v1/
```

The release build of the app reads from that base URL (`PRICES_API_BASE_URL` in
`app/build.gradle.kts`). There is no server code, no key and no sign-in. GitHub serves these files
with `Cache-Control: max-age=300`, so a change pushed to `main` can take up to five minutes to
appear.

**The prices are sample data: realistic, not real.** They are in the shape of the APMC daily prices
the Government of India publishes on data.gov.in, but they are made up for the course. Do not trade
on them.

## Files

| Path | What it is | When the app downloads it |
|---|---|---|
| `v1/prices.json` | The newest day's prices at every market, with each market's district | Every refresh |
| `v1/history.json` | Modal prices for the seven days before the newest day | On first launch, or when the phone's newest saved day is more than a day old |
| `v1/days/2026-11-07.json`, `v1/days/2026-11-08.json` | Each day's file, kept. `prices.json` is always a copy of the newest | Never; for trying a new day locally (see below) |
| `v1/images/thumb/<commodity>.jpg` | 160 × 160 pictures, about 7 KB each, for the price cards | When a card is shown, then from Coil's disk cache |
| `v1/images/photo/<commodity>.jpg` | 960 × 640 pictures, about 100 KB each, for the detail screen | When a detail screen is opened |

## The schema, version 1

`prices.json`:

```json
{
  "schema_version": 1,
  "date": "2026-11-08",
  "published_at": "2026-11-08T10:35:00Z",
  "unit": "rupees per quintal",
  "source": "…",
  "markets": [{ "name": "Lasalgaon APMC", "district": "Nashik", "state": "Maharashtra" }],
  "prices": [
    {
      "id": "lasalgaon-onion-2026-11-08",
      "commodity": "Onion",
      "market": "Lasalgaon APMC",
      "date": "2026-11-08",
      "min_price": 1250,
      "max_price": 2950,
      "modal_price": 2580,
      "thumbnail": "images/thumb/onion.jpg",
      "photo": "images/photo/onion.jpg"
    }
  ]
}
```

`history.json` has `schema_version`, `from`, `to`, `unit`, `source`, and `history`: a list of
`{ "market", "commodity", "date", "modal_price" }`.

Rules for changing it:

- Prices are whole rupees per quintal; dates are ISO `yyyy-MM-dd`.
- A price's `id` is `<market>-<commodity>-<date>` and is unique.
- Picture paths are relative to the `v1/` folder.
- **Adding a field is allowed in v1.** The app ignores keys it does not know.
- **Renaming, removing or retyping a field is not.** That is `v2/`, in a new folder, with `v1/`
  left in place for the copies of the app already installed.

## Running it on your computer

From the repository's root folder:

```bash
python3 -m http.server 8765
```

and add `mandibhav.apiBaseUrl=http://10.0.2.2:8765/api/v1/` to `local.properties`. Debug builds
then read from your computer (`10.0.2.2` is the emulator's name for it), over plain HTTP, which only
debug builds allow and only to that address. To see a new day arrive, start with a copy of
`days/2026-11-07.json` as `prices.json`, open the app, then put `days/2026-11-08.json` back and tap
Refresh.

## Where the pictures came from

Every picture was drawn for this repository by `tools/draw_commodity_images.py`, a Pillow script
that draws a heap of produce on a hessian sack. Nothing was downloaded or copied from elsewhere, so
there is no third-party licence to follow. Run the script again to redraw them; the output is the
same every time.
