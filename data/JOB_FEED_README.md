# Job Feed for JavaFX

## Output

- **File**: `job_feed.json`
- **Updated by**: `python/job_feed_crawler.py`

## Schema

```json
{
  "updated": "2026-01-29T21:19:19+00:00",
  "count": 150,
  "jobs": [
    {
      "source": "ANETI",
      "title": "...",
      "url": "https://...",
      "description": "",
      "location": "Tunisia",
      "posted_date": "2026-01-29",
      "raw_id": "..."
    }
  ]
}
```

## Sources

- **ANETI**: emploi.nat.tn (Tunisia jobs). Flow: page 147 (Offres d'emploi) → entry points 148 (cadres), 152 (autres), 163 (formation pro) → listing page 146; jobs from table `#menuTable`, detail URL `global.php?page=990&bureau=X&annee=Y&numoffre=Z`.
- **Reddit**: r/jobs (filtered), r/RemoteJobs, r/forhire, r/jobbit
- **RSS**: Remote OK, We Work Remotely

Optional: set env `SKILORA_RSS_URLS=Display Name|https://...` to add feeds (e.g. LinkedIn via IFTTT).

## Run crawler

```bash
cd python
pip install -r requirements_jobs.txt
python job_feed_crawler.py
```

**Options:** `--config path`, `--max N`.  
**Config:** `python/job_feed_config.json`. Dependencies (aiohttp, beautifulsoup4, feedparser, lxml) are auto-installed if missing (Windows/Linux/macOS/VPS).

## Load in JavaFX

Read `data/job_feed.json` (path relative to project root or user.dir). Parse with Gson/Jackson or `javax.json`; bind `jobs` to your table/list. Use `source`, `title`, `url`, `location`, `posted_date` for display; open `url` in browser on row click.
