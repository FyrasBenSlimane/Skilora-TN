#!/usr/bin/env python3
"""
Job feed crawler: ANETI (emploi.nat.tn) + RSS sources.
Output: ../data/job_feed.json (or path from --config).
Run: python job_feed_crawler.py [--config path] [--max N]
"""
import argparse
import json
import re
import sys
from pathlib import Path
from datetime import datetime
from urllib.request import urlopen, Request
from urllib.error import URLError

try:
    import feedparser
except ImportError:
    feedparser = None

OUTPUT_DIR = Path(__file__).resolve().parent.parent / "data"
OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
CONFIG_PATH = Path(__file__).resolve().parent / "job_feed_config.json"


def load_config(path):
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def parse_rss(url, name, max_per_feed=30):
    """Parse RSS feed and return list of job entries."""
    if not feedparser:
        return []
    try:
        feed = feedparser.parse(url)
        entries = (feed.get("entries") or [])[:max_per_feed]
        out = []
        for e in entries:
            title = e.get("title") or ""
            link = e.get("link") or ""
            summary = e.get("summary", "") or ""
            if isinstance(summary, dict):
                summary = summary.get("value", "") or ""
            published = e.get("published", "") or ""
            if published and hasattr(published, "split"):
                published = published.split("T")[0] if "T" in published else published[:10]
            out.append({
                "source": name,
                "title": title,
                "url": link,
                "description": re.sub("<[^>]+>", " ", summary).strip()[:500],
                "location": "Remote",
                "posted_date": published or datetime.now().strftime("%Y-%m-%d"),
                "raw_id": link or title[:50]
            })
        return out
    except Exception as e:
        print(f"[RSS] {name} error: {e}", file=sys.stderr)
        return []


def fetch_aneti_sample(max_per=50):
    """Return sample ANETI-style jobs (real ANETI scraping would need session/API)."""
    return [
        {"source": "ANETI", "title": "Ingénieur / Ingénieure génie civil", "url": "https://www.emploi.nat.tn/fo/Fr/global.php?page=990&bureau=2110&annee=2026&numoffre=103", "description": "Conception et suivi de projets.", "location": "NABEUL", "posted_date": datetime.now().strftime("%Y-%m-%d"), "raw_id": "2110/2026/103"},
        {"source": "ANETI", "title": "Développeur / Développeuse Java", "url": "https://www.emploi.nat.tn/fo/Fr/global.php?page=990&bureau=1210&annee=2026&numoffre=200", "description": "Java, Spring Boot, SQL.", "location": "ARIANA", "posted_date": datetime.now().strftime("%Y-%m-%d"), "raw_id": "1210/2026/200"},
        {"source": "ANETI", "title": "Chargé / Chargée d'affaires BTP", "url": "https://www.emploi.nat.tn/fo/Fr/global.php?page=990&bureau=6140&annee=2026&numoffre=542", "description": "Gestion dossiers BTP.", "location": "SFAX", "posted_date": datetime.now().strftime("%Y-%m-%d"), "raw_id": "6140/2026/542"},
        {"source": "ANETI", "title": "Responsable service informatique", "url": "https://www.emploi.nat.tn/fo/Fr/global.php?page=990&bureau=5110&annee=2026&numoffre=498", "description": "Pilotage équipe informatique.", "location": "SOUSSE", "posted_date": datetime.now().strftime("%Y-%m-%d"), "raw_id": "5110/2026/498"},
        {"source": "ANETI", "title": "Développeur / Développeuse Python", "url": "https://www.emploi.nat.tn/fo/Fr/global.php?page=990&bureau=1320&annee=2026&numoffre=310", "description": "Backend Python, API REST.", "location": "BEN AROUS", "posted_date": datetime.now().strftime("%Y-%m-%d"), "raw_id": "1320/2026/310"},
        {"source": "ANETI", "title": "Designer UX/UI", "url": "https://www.emploi.nat.tn/fo/Fr/global.php?page=990&bureau=2120&annee=2026&numoffre=350", "description": "Interfaces, Figma.", "location": "NABEUL", "posted_date": datetime.now().strftime("%Y-%m-%d"), "raw_id": "2120/2026/350"},
    ][:max_per]


def main():
    ap = argparse.ArgumentParser(description="Job feed crawler (ANETI + RSS)")
    ap.add_argument("--config", default=str(CONFIG_PATH), help="Config JSON path")
    ap.add_argument("--max", type=int, default=200, help="Max jobs total")
    args = ap.parse_args()
    config = load_config(args.config) if Path(args.config).exists() else {}
    rss_c = (config.get("rss") or {})
    rss_feeds = rss_c.get("feeds") or []
    max_per_feed = int(rss_c.get("max_per_feed") or 30)
    aneti_c = config.get("aneti") or {}
    aneti_max = int(aneti_c.get("max_per_source") or 100) if aneti_c.get("enabled", True) else 0

    all_jobs = []
    if aneti_max:
        all_jobs.extend(fetch_aneti_sample(aneti_max))
    for name, url in rss_feeds[:10]:
        all_jobs.extend(parse_rss(url, name, max_per_feed))

    out_path = config.get("output") or str(OUTPUT_DIR / "job_feed.json")
    if not out_path.startswith("/") and ":" not in out_path:
        out_path = str(Path(__file__).resolve().parent.parent / out_path)
    payload = {
        "updated": datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%S+00:00"),
        "count": len(all_jobs),
        "jobs": all_jobs[: args.max]
    }
    Path(out_path).parent.mkdir(parents=True, exist_ok=True)
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(payload, f, ensure_ascii=False, indent=2)
    print(f"Written {len(payload['jobs'])} jobs to {out_path}")


if __name__ == "__main__":
    main()
