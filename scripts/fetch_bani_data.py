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
    (4, "jaap-sahib"),
    (31, "sukhmani-sahib"),
    (3, "shabad-hazare"),
    (9, "chaupai-sahib"),
    (21, "rehras-sahib"),
    (23, "kirtan-sohila"),
    (10, "anand-sahib"),
    (6, "tavprasad-savaiye-sraavag-sudh"),
    (7, "tavprasad-savaiye-deenan-kee"),
]


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

    paragraphs = []
    current_paragraph = None
    current_para_id = None

    for verse in verses:
        v = verse.get("verse", {})
        header = verse.get("header", 0)
        para_id = verse.get("paragraph", 0)

        gurmukhi = v.get("verse", {}).get("unicode", "")
        transliteration = v.get("transliteration", {})
        en = transliteration.get("english", "")
        hi = transliteration.get("hindi", "")

        line = {"pn": gurmukhi, "en": en, "hi": hi}

        # Determine section name for headers (Sukhmani Sahib astpadis etc.)
        section = None
        if header in (1, 2) and gurmukhi.strip():
            section = en.strip() if en.strip() else gurmukhi.strip()

        # Group by paragraph ID
        if para_id != current_para_id:
            current_paragraph = {"lines": [line]}
            if section:
                current_paragraph["section"] = section
            paragraphs.append(current_paragraph)
            current_para_id = para_id
        else:
            current_paragraph["lines"].append(line)
            # If this line has a section and the paragraph doesn't yet, set it
            if section and "section" not in current_paragraph:
                current_paragraph["section"] = section

    return {
        "id": bani_info.get("baniID", 0),
        "nameEn": bani_info.get("transliteration", {}).get("english", slug),
        "slug": slug,
        "paragraphs": paragraphs,
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
            line_count = sum(len(p["lines"]) for p in data["paragraphs"])
            print(f"  -> {len(data['paragraphs'])} paragraphs, {line_count} lines")
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
