#!/usr/bin/env python3
"""
Job Feed Crawler – ANETI (TN), Reddit, RSS. Blazing path only (aiohttp concurrent I/O).
Run: python job_feed_crawler.py [--config path] [--max N]
Output: ../data/job_feed.json
Dependencies: auto-installed if missing (Windows / Linux / macOS / VPS).
"""

import argparse
import html
import os
import json
import re
import subprocess
import sys
from dataclasses import asdict, dataclass
from datetime import datetime, timezone
from pathlib import Path
from urllib.parse import parse_qs, urlparse
from typing import Any

# (import name, pip package name)
_REQUIRED_DEPS = [("aiohttp", "aiohttp"), ("bs4", "beautifulsoup4"), ("feedparser", "feedparser"), ("lxml", "lxml")]


def _ensure_deps() -> None:
    """Check required packages and auto-install missing ones (Windows/Linux/macOS/VPS)."""
    missing = []
    for mod, pkg in _REQUIRED_DEPS:
        try:
            __import__(mod)
        except ImportError:
            missing.append(pkg)
    if not missing:
        return
    # Use same Python; on Linux/macOS outside venv use --user to avoid sudo
    in_venv = getattr(sys, "prefix", None) != getattr(sys, "base_prefix", None)
    cmd = [sys.executable, "-m", "pip", "install", "-q", "--disable-pip-version-check"]
    if not in_venv and sys.platform != "win32":
        cmd.append("--user")
    cmd.extend(missing)
    try:
        subprocess.run(cmd, check=True, timeout=120)
    except (subprocess.CalledProcessError, FileNotFoundError, OSError) as e:
        print(f"Auto-install failed: {e}. Install manually: pip install {' '.join(missing)}", file=sys.stderr)
        sys.exit(1)


_ensure_deps()

import asyncio

import aiohttp
from bs4 import BeautifulSoup
import feedparser


@dataclass
class JobEntry:
    source: str
    title: str
    url: str
    description: str
    location: str
    posted_date: str
    apply_url: str = ""
    raw_id: str = ""


USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; rv:91.0) Gecko/20100101 Firefox/91.0 SkiloraJobCrawler/1.0"
REDDIT_USER_AGENT = "SkiloraJobCrawler/1.0 (by /u/skilora)"
OUTPUT_DIR = Path(__file__).resolve().parent.parent / "data"
OUTPUT_FILE = OUTPUT_DIR / "job_feed.json"
CONFIG_FILE = Path(__file__).resolve().parent / "job_feed_config.json"

DEFAULT_CONFIG: dict[str, Any] = {
    "aneti": {"enabled": True, "listing_url": "https://www.emploi.nat.tn/fo/Fr/global.php?page=146&FormLinks_Sorting=7&FormLinks_Sorted=7", "max_jobs": 0},
    "reddit": {"enabled": True, "subreddits": ["jobs", "RemoteJobs", "forhire", "jobbit"], "max_per_sub": 25},
    "rss": {"enabled": True, "feeds": [["Remote OK (RSS)", "https://remoteok.com/remote-jobs.rss"], ["We Work Remotely", "https://weworkremotely.com/categories/remote-programming-jobs.rss"]], "max_per_feed": 30},
    "timeout": 8,
    "max_total_per_source": 0,
    "validate_links": False,
    "validate_max": 0,  # 0 = validate all
    "validate_concurrency": 24,
    "validate_timeout": 6,
}


def load_config(path: Path | None = None) -> dict[str, Any]:
    p = path or Path(os.environ.get("SKILORA_JOB_CONFIG", "")) or CONFIG_FILE
    if p.is_file():
        try:
            cfg = json.loads(p.read_text(encoding="utf-8"))
            for k, v in DEFAULT_CONFIG.items():
                if k not in cfg:
                    cfg[k] = v
            return cfg
        except Exception:
            pass
    return dict(DEFAULT_CONFIG)


# ---------- ANETI (emploi.nat.tn) ----------
# Flow from page 147: "Offres d'emploi" hub → 3 entry points (javascript:offre(1/2/3))
# submit to page 148 (cadres), 152 (autres), 163 (formation pro) → "Liste des offres" → page 146.
# Page 146: table#menuTable with job rows; detail URL = global.php?page=990&bureau=X&annee=Y&numoffre=Z
ANETI_BASE = "https://www.emploi.nat.tn/fo/Fr"
ANETI_LISTING_URL = f"{ANETI_BASE}/global.php?page=146&FormLinks_Sorting=7&FormLinks_Sorted=7"


def _decode_aneti(raw: bytes) -> str:
    for enc in ("cp1256", "windows-1256", "iso-8859-1", "utf-8", "cp1252"):
        try:
            return raw.decode(enc, errors="strict")
        except (LookupError, UnicodeDecodeError):
            continue
    return raw.decode("utf-8", errors="replace")


# Reddit: only include job-listing-like posts (not general career discussion)
JOB_SUBREDDITS = ["RemoteJobs", "forhire", "jobbit"]  # these are job boards
DISCUSSION_SUBREDDIT = "jobs"  # filter by keywords
JOB_TITLE_KEYWORDS = re.compile(
    r"\[?\s*hiring\s*\]?|hiring\s*[!:\.]|we're?\s+hiring|job\s+opening|position\s+open|"
    r"remote\s+position|full[- ]?time|part[- ]?time|contractor|freelance|"
    r"\[for\s+hire\]|\[hire\s+me\]|looking\s+for\s+(a\s+)?(developer|engineer|writer)",
    re.I,
)


def _is_job_post(sub: str, title: str, link_flair_text: str | None) -> bool:
    if sub in JOB_SUBREDDITS:
        return True
    if sub == DISCUSSION_SUBREDDIT:
        if link_flair_text and "hiring" in link_flair_text.lower():
            return True
        return bool(JOB_TITLE_KEYWORDS.search(title))
    return False


# ---------- RSS ----------
def _rss_sources(config: dict[str, Any] | None) -> list[tuple[str, str]]:
    cfg = config or load_config()
    feeds = (cfg.get("rss") or {}).get("feeds") or []
    out = [tuple(f)[:2] for f in feeds if isinstance(f, (list, tuple)) and len(f) >= 2]
    env = os.environ.get("SKILORA_RSS_URLS", "").strip()
    if env:
        for part in env.split(","):
            part = part.strip()
            if "|" in part:
                name, url = part.split("|", 1)
                out.append((name.strip(), url.strip()))
    return out or [("Remote OK (RSS)", "https://remoteok.com/remote-jobs.rss"), ("We Work Remotely", "https://weworkremotely.com/categories/remote-programming-jobs.rss")]


def _cap_source(jobs: list[JobEntry], max_total: int) -> list[JobEntry]:
    if max_total <= 0:
        return jobs
    return jobs[:max_total]


# ---------- Blazing async (aiohttp) ----------
async def _fetch(session: "aiohttp.ClientSession", url: str, timeout: int, ssl: bool = True) -> tuple[str, bytes]:
    try:
        async with session.get(url, timeout=aiohttp.ClientTimeout(total=timeout), ssl=ssl) as r:
            r.raise_for_status()
            body = await r.read()
            return (url, body)
    except Exception as e:
        print(f"[blazing] {url[:50]}... Error: {e}")
        return (url, b"")


def _parse_aneti_body(body: bytes, max_jobs: int, timeout: int) -> list[JobEntry]:
    jobs = []
    if not BeautifulSoup:
        return jobs
    text = _decode_aneti(body)
    soup = BeautifulSoup(text, "lxml")
    table = soup.find("table", id="menuTable")
    if not table:
        return jobs
    tbody = table.find("tbody") or table
    for tr in tbody.find_all("tr", class_="emp"):
        bureau = annee = numoffre = None
        ref_a = tr.find("a", href=re.compile(r"page=990.*bureau="))
        href = ""
        if ref_a and ref_a.get("href"):
            href = (ref_a.get("href", "") or "").strip()
            parsed = urlparse(href)
            qs = parse_qs(parsed.query)
            bureau = (qs.get("bureau") or [""])[0]
            annee = (qs.get("annee") or [""])[0]
            numoffre = (qs.get("numoffre") or [""])[0]
        title_a = tr.find("td", class_="profession")
        title_a = title_a.find("a") if title_a else None
        if not title_a:
            title_a = tr.find("a", onclick=re.compile(r"show_detail\s*\("))
        if title_a and not (bureau and numoffre):
            m = re.search(r"show_detail\s*\(\s*['\"]([^'\"]+)['\"]\s*,\s*['\"]([^'\"]+)['\"]\s*,\s*['\"]([^'\"]+)['\"]", title_a.get("onclick") or "")
            if m:
                bureau, annee, numoffre = m.group(1), m.group(2), m.group(3)
        if not bureau or not numoffre:
            continue
        title = (title_a.get_text() or "").strip()[:200] if title_a else ""
        if not title and ref_a:
            title = (ref_a.get_text() or "").strip()[:200]
        if not title:
            continue
        # Prefer the exact href from the listing (includes cpt/fin params),
        # falling back to a reconstructed URL if needed.
        if href and "global.php" in href:
            job_url = href if href.startswith("http") else f"{ANETI_BASE}/{href.lstrip('/')}"
        else:
            job_url = f"{ANETI_BASE}/global.php?page=990&bureau={bureau}&annee={annee}&numoffre={numoffre}"

        loc = "Tunisia"
        td_service = tr.find("td", class_="service")
        if td_service:
            t = (td_service.get_text() or "").strip()
            if t:
                loc = t[:80]
        posted = datetime.now(timezone.utc).strftime("%Y-%m-%d")
        td_cells = tr.find_all("td")
        if len(td_cells) > 5:
            dt = (td_cells[5].get_text() or "").strip()
            if dt and re.match(r"\d{2}/\d{2}/\d{4}", dt):
                try:
                    day, month, year = dt.split("/")
                    posted = f"{year}-{month}-{day}"
                except Exception:
                    pass
        # The listing row includes useful fields (diploma, domain, ...). Embed a compact summary
        # so ANETI jobs have non-empty details even if the detail page requires JS.
        diploma = domain = ""
        try:
            td_cells = tr.find_all("td")
            if len(td_cells) > 6:
                diploma = " ".join((td_cells[6].get_text() or "").split())[:140]
            if len(td_cells) > 7:
                domain = " ".join((td_cells[7].get_text() or "").split())[:140]
        except Exception:
            pass
        summary_parts = []
        if diploma:
            summary_parts.append(f"Diplôme: {diploma}")
        if domain:
            summary_parts.append(f"Domaine: {domain}")
        summary = " • ".join(summary_parts)

        jobs.append(JobEntry(
            source="ANETI",
            title=title,
            url=job_url,
            apply_url=job_url,
            description=summary,
            location=loc,
            posted_date=posted,
            raw_id=f"{bureau}/{annee}/{numoffre}",
        ))
        if max_jobs and len(jobs) >= max_jobs:
            break
    return jobs


def _parse_reddit_body(body: bytes, sub: str) -> list[JobEntry]:
    jobs = []
    if not body:
        return jobs
    try:
        data = json.loads(body.decode("utf-8"))
        for child in data.get("data", {}).get("children", []):
            d = child.get("data", {})
            title = (d.get("title") or "").strip()
            if not title or not _is_job_post(sub, title, d.get("link_flair_text")):
                continue
            link = "https://www.reddit.com" + (d.get("permalink") or "")
            raw_selftext = (d.get("selftext") or "")
            selftext = raw_selftext[:500]
            created = d.get("created_utc")
            posted = datetime.fromtimestamp(created, tz=timezone.utc).strftime("%Y-%m-%d") if created else datetime.now(timezone.utc).strftime("%Y-%m-%d")
            # Prefer a real external job link when the post contains one.
            apply_url = ""
            try:
                overridden = (d.get("url_overridden_by_dest") or d.get("url") or "").strip()
                if overridden and "reddit.com" not in overridden:
                    apply_url = overridden
                if not apply_url:
                    candidates = [
                        html.unescape(u).strip().strip(").,]")
                        for u in re.findall(r"https?://\S+", raw_selftext)
                    ]
                    candidates = [u for u in candidates if u and "reddit.com" not in u]
                    if candidates:
                        def score(u: str) -> int:
                            try:
                                p = urlparse(u)
                                has_path = bool(p.path and p.path != "/")
                            except Exception:
                                has_path = "/" in u[8:]
                            s = 0
                            if has_path:
                                s += 10
                            lu = u.lower()
                            if "job" in lu:
                                s += 5
                            if "apply" in lu or "careers" in lu:
                                s += 3
                            return s

                        apply_url = max(candidates, key=score)
            except Exception:
                apply_url = ""

            # Normalize HTML entities (e.g., &amp;)
            if apply_url:
                apply_url = html.unescape(apply_url).strip()

            jobs.append(JobEntry(
                source=f"Reddit r/{sub}",
                title=title[:200],
                url=link,
                apply_url=apply_url,
                description=selftext,
                location="",
                posted_date=posted,
                raw_id=d.get("id", ""),
            ))
    except Exception as e:
        print(f"[Reddit r/{sub}] Error: {e}")
    return jobs


def _parse_rss_body(body: bytes, name: str, max_per_feed: int) -> list[JobEntry]:
    jobs = []
    if not feedparser or not body:
        return jobs
    try:
        text = body.decode("utf-8", errors="replace") if isinstance(body, bytes) else body
        feed = feedparser.parse(text)
        entries = (feed.get("entries") or [])[:max_per_feed]
        for e in entries:
            title = (e.get("title") or "").strip()
            link = e.get("link") or ""
            if not title or not link:
                continue
            raw_desc = e.get("summary") or e.get("description") or ""
            if hasattr(raw_desc, "value"):
                raw_desc = raw_desc.value
            desc = re.sub(r"<[^>]+>", " ", str(raw_desc)[:500])
            published = e.get("published_parsed") or e.get("updated_parsed")
            if published:
                try:
                    posted = datetime(*published[:6]).strftime("%Y-%m-%d")
                except Exception:
                    posted = datetime.now(timezone.utc).strftime("%Y-%m-%d")
            else:
                posted = datetime.now(timezone.utc).strftime("%Y-%m-%d")
            location = next((str(e[k])[:100] for k in ("location", "geo_city", "city") if e.get(k)), "")
            jobs.append(JobEntry(
                source=name,
                title=title[:200],
                url=link,
                apply_url=link,
                description=desc,
                location=location,
                posted_date=posted,
                raw_id=e.get("id", link),
            ))
    except Exception as e:
        print(f"[RSS {name}] Error: {e}")
    return jobs


# ---------- URL validation ----------

_DENY_URL_SUBSTRINGS = [
    "emploi.nat.tn/blackhole",
]


def _is_denied_url(url: str) -> bool:
    u = (url or "").lower()
    return any(s in u for s in _DENY_URL_SUBSTRINGS)


async def _validate_one(session: "aiohttp.ClientSession", url: str, timeout: int) -> tuple[bool, str, int]:
    """
    Validate a URL without downloading full content.
    Returns: (ok, final_url, status)
    """
    if not url or _is_denied_url(url):
        return (False, url or "", 0)
    try:
        # HEAD first (cheap); some sites block HEAD, so fall back to GET.
        async with session.request("HEAD", url, allow_redirects=True, timeout=aiohttp.ClientTimeout(total=timeout)) as r:
            status = int(r.status)
            final_url = str(r.url)
            if status in (401, 403, 429):
                return (True, final_url, status)  # soft-valid (blocked/rate-limited)
            if 200 <= status < 400:
                return (True, final_url, status)
            if status in (405, 501):
                raise RuntimeError("HEAD not allowed")
            return (False, final_url, status)
    except Exception:
        try:
            async with session.get(url, allow_redirects=True, timeout=aiohttp.ClientTimeout(total=timeout)) as r:
                status = int(r.status)
                final_url = str(r.url)
                # Read a tiny chunk then stop
                try:
                    await r.content.read(256)
                except Exception:
                    pass
                if status in (401, 403, 429):
                    return (True, final_url, status)
                if 200 <= status < 400:
                    return (True, final_url, status)
                return (False, final_url, status)
        except Exception:
            return (False, url, 0)


async def _validate_jobs_async(jobs: list[JobEntry], cfg: dict[str, Any]) -> list[JobEntry]:
    timeout = int(cfg.get("validate_timeout") or 6)
    limit = int(cfg.get("validate_max") or 0)
    concurrency = int(cfg.get("validate_concurrency") or 24)

    # Validate apply_url if present, else url.
    targets: list[tuple[int, str]] = []
    for idx, j in enumerate(jobs):
        target = (j.apply_url or j.url or "").strip()
        target = html.unescape(target).strip()
        if not target:
            continue
        if _is_denied_url(target):
            continue
        targets.append((idx, target))

    if limit > 0:
        targets = targets[:limit]

    if not targets:
        return jobs

    headers = {"User-Agent": USER_AGENT, "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"}

    sem = asyncio.Semaphore(concurrency)

    connector = aiohttp.TCPConnector(limit=concurrency, limit_per_host=max(2, concurrency // 6))
    async with aiohttp.ClientSession(headers=headers, connector=connector) as session:
        async def worker(i: int, u: str) -> tuple[int, bool, str, int]:
            async with sem:
                ok, final_url, status = await _validate_one(session, u, timeout)
                return (i, ok, final_url, status)

        results = await asyncio.gather(*(worker(i, u) for i, u in targets))

    bad_idx: set[int] = set()
    for i, ok, final_url, _status in results:
        if ok:
            if 0 <= i < len(jobs):
                # Update apply_url to final (post-redirect) URL when possible
                if jobs[i].apply_url:
                    jobs[i].apply_url = final_url
        else:
            bad_idx.add(i)

    if not bad_idx:
        return jobs
    return [j for i, j in enumerate(jobs) if i not in bad_idx]


async def _blazing_crawl_async(config: dict[str, Any]) -> tuple[list[JobEntry], list[JobEntry], list[JobEntry]]:
    ac = config.get("aneti", {})
    rc = config.get("reddit", {})
    rss_c = config.get("rss", {})
    timeout = int(config.get("timeout") or 5)
    aneti_url = (ac.get("listing_url") or "").strip() or ANETI_LISTING_URL
    subreddits = rc.get("subreddits") or [DISCUSSION_SUBREDDIT] + JOB_SUBREDDITS
    max_per_sub = int(rc.get("max_per_sub") or 25)
    sources = _rss_sources(config)
    max_per_feed = int(rss_c.get("max_per_feed") or 30)
    max_aneti = int(ac.get("max_jobs") or 0)

    connector = aiohttp.TCPConnector(limit=16, limit_per_host=4)
    headers = {"User-Agent": USER_AGENT, "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"}
    reddit_headers = {"User-Agent": REDDIT_USER_AGENT}

    async with aiohttp.ClientSession(headers=headers, connector=connector) as session:
        async with aiohttp.ClientSession(headers=reddit_headers, connector=connector) as rsession:
            tasks = [_fetch(session, aneti_url, timeout, ssl=False)]
            for sub in subreddits:
                tasks.append(_fetch(rsession, f"https://www.reddit.com/r/{sub}/new.json?limit={max_per_sub}", timeout, ssl=True))
            for _name, url in sources:
                tasks.append(_fetch(session, url, timeout, ssl=True))
            results = await asyncio.gather(*tasks)
    # Parse: [0]=ANETI, [1:1+N]=Reddit, [1+N:]=RSS
    n_reddit = len(subreddits)
    aneti_body = results[0][1] if results else b""
    aneti_jobs = _parse_aneti_body(aneti_body, max_aneti, timeout) if ac.get("enabled", True) and aneti_body else []
    reddit_jobs = []
    for i in range(n_reddit):
        if 1 + i < len(results):
            reddit_jobs.extend(_parse_reddit_body(results[1 + i][1], subreddits[i]))
    rss_jobs = []
    for i, (name, _) in enumerate(sources):
        idx = 1 + n_reddit + i
        if idx < len(results):
            rss_jobs.extend(_parse_rss_body(results[idx][1], name, max_per_feed))
    return (aneti_jobs, reddit_jobs, rss_jobs)


def main() -> None:
    parser = argparse.ArgumentParser(description="Job feed crawler (ANETI, Reddit, RSS) – blazing concurrent I/O")
    parser.add_argument("--config", "-c", type=Path, default=None, help="Config JSON path")
    parser.add_argument("--max", "-m", type=int, default=0, help="Cap total jobs per source (0 = no cap)")
    parser.add_argument("--validate-links", action="store_true", help="Validate URLs (drop broken listings)")
    parser.add_argument("--validate-max", type=int, default=None, help="Max URLs to validate (0 = all)")
    args = parser.parse_args()

    config = load_config(args.config)
    if args.max > 0:
        config["max_total_per_source"] = args.max
    if args.validate_links:
        config["validate_links"] = True
    if args.validate_max is not None:
        config["validate_max"] = int(args.validate_max)

    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    max_per_source = int(config.get("max_total_per_source") or 0)

    aneti_jobs, reddit_jobs, rss_jobs = asyncio.run(_blazing_crawl_async(config))
    aneti_jobs = _cap_source(aneti_jobs, max_per_source)
    reddit_jobs = _cap_source(reddit_jobs, max_per_source)
    rss_jobs = _cap_source(rss_jobs, max_per_source)

    print(f"ANETI: {len(aneti_jobs)} jobs")
    print(f"Reddit: {len(reddit_jobs)} jobs")
    print(f"RSS: {len(rss_jobs)} jobs")

    all_jobs = aneti_jobs + reddit_jobs + rss_jobs
    seen: set[str] = set()
    unique: list[JobEntry] = []
    for j in all_jobs:
        if j.url not in seen:
            seen.add(j.url)
            unique.append(j)

    if config.get("validate_links"):
        try:
            before = len(unique)
            unique = asyncio.run(_validate_jobs_async(unique, config))
            after = len(unique)
            print(f"Validated links: kept {after}/{before}")
        except Exception as e:
            print(f"[validate] Error: {e}")

    out = {
        "updated": datetime.now(timezone.utc).isoformat(),
        "count": len(unique),
        "jobs": [asdict(j) for j in unique],
    }
    OUTPUT_FILE.write_text(json.dumps(out, indent=2, ensure_ascii=False), encoding="utf-8")
    print(f"Total: {len(unique)} jobs -> {OUTPUT_FILE}")


if __name__ == "__main__":
    main()
