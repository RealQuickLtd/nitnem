#!/usr/bin/env python3
"""Fetch bani data from BaniDB API and generate JSON files for the app.

Usage:
    python3 scripts/fetch_bani_data.py

Output:
    app/src/main/assets/banis/<slug>.json for each bani
"""

import json
import os
import sys
import urllib.request

API_BASE = "https://api.banidb.com/v2/banis"
OUTPUT_DIR = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "app", "src", "main", "assets", "banis"
)

BANIS = [
    (2, "japji-sahib"),
    (3, "shabad-hazare"),
    (4, "jaap-sahib"),
    (6, "tavprasad-savaiye-sraavag-sudh"),
    (7, "tavprasad-savaiye-deenan-kee"),
    (9, "chaupai-sahib"),
    (10, "anand-sahib"),
    (21, "rehras-sahib"),
    (22, "aarti"),
    (23, "kirtan-sohila"),
    (24, "ardas"),
    (31, "sukhmani-sahib"),
    (90, "asa-di-vaar"),
]

FORCE_MEDIUM_PARAGRAPHS = {
    "chaupai-sahib": {42, 43, 44, 45},
}


def fetch_bani(bani_id: int) -> dict:
    url = f"{API_BASE}/{bani_id}"
    print(f"  Fetching {url}")
    req = urllib.request.Request(url, headers={"User-Agent": "NitnemDataFetcher/1.0"})
    with urllib.request.urlopen(req, timeout=30) as resp:
        return json.loads(resp.read().decode())


def transform(raw: dict, slug: str) -> dict:
    """Transform BaniDB API response into the app's JSON format."""
    bani_info = raw.get("baniInfo", {})
    verses = raw.get("verses", [])

    full_verses = []

    for verse in verses:
        v = verse.get("verse", {})
        header = verse.get("header", 0)
        para_id = verse.get("paragraph", 0)
        mangal_position = verse.get("mangalPosition")

        # Match STTM's Sundar Gutka behavior: exclude verses placed "above"
        # the current shabad header from the normal reading flow.
        if mangal_position == "above":
            continue

        gurmukhi = v.get("verse", {}).get("unicode", "")
        transliteration = v.get("transliteration", {})
        en = transliteration.get("english", "")
        hi = transliteration.get("hindi", "")

        # Determine section name for headers (Sukhmani, Asa Di Vaar, etc.)
        section = None
        if header in (1, 2) and gurmukhi.strip():
            section = en.strip() if en.strip() else gurmukhi.strip()

        exists_short = bool(verse.get("existsSGPC", 1))
        exists_medium = bool(verse.get("existsMedium", 1))
        exists_long = bool(verse.get("existsTaksal", 1))
        exists_extra_long = bool(verse.get("existsBuddhaDal", 1))

        if para_id in FORCE_MEDIUM_PARAGRAPHS.get(slug, set()):
            exists_medium = True
            exists_long = True
            exists_extra_long = True

        entry = {
            "p": para_id,
            "pn": gurmukhi,
            "en": en,
            "hi": hi,
        }

        if not exists_short:
            entry["es"] = False
        if exists_medium != exists_short:
            entry["em"] = exists_medium
        if exists_long != exists_medium:
            entry["el"] = exists_long
        if exists_extra_long != exists_long:
            entry["ex"] = exists_extra_long
        if section:
            entry["s"] = section
        full_verses.append(entry)

    return {
        "id": bani_info.get("baniID", 0),
        "nameEn": bani_info.get("english", slug),
        "slug": slug,
        "verses": full_verses,
    }


def main():
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    errors = []

    for bani_id, slug in BANIS:
        print(f"Processing {slug} (ID: {bani_id})...")
        try:
            raw = fetch_bani(bani_id)
            data = transform(raw, slug)
            out_path = os.path.join(OUTPUT_DIR, f"{slug}.json")
            with open(out_path, "w", encoding="utf-8") as f:
                json.dump(data, f, ensure_ascii=False, indent=2)
            print(f"  -> {len(data['verses'])} verses")
        except Exception as e:
            print(f"  ERROR: {e}", file=sys.stderr)
            errors.append((slug, str(e)))

    print(f"\nDone. {len(BANIS) - len(errors)}/{len(BANIS)} banis fetched.")
    if errors:
        print("Errors:")
        for slug, err in errors:
            print(f"  {slug}: {err}")
        sys.exit(1)


if __name__ == "__main__":
    main()
