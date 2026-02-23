"""
Skilora Recruitment AI API
=========================
- Smart Matching (TF-IDF + cosine similarity + keyword overlap)
- CV Analysis (PDF extraction + NLP skill detection)
- Job Recommendations (collaborative-style scoring)
- Candidate Scoring (experience + skills + certifications)
- WhatsApp notifications (interview scheduled – via Twilio)
"""

from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Optional
import PyPDF2
import io
import re
import math
from twilio.rest import Client
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
import os
from dotenv import load_dotenv
import json

load_dotenv()

app = FastAPI(
    title="Skilora AI Recruitment API",
    description="Intelligent recruitment APIs: matching, CV analysis, recommendations, scoring.",
    version="2.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# =============================================
# MODELS
# =============================================

class MatchRequest(BaseModel):
    candidate_skills: List[str]
    job_skills: List[str]
    candidate_experience_years: Optional[int] = 0
    required_experience_level: Optional[str] = "JUNIOR"

class MatchResponse(BaseModel):
    match_percentage: int
    matched_skills: List[str]
    missing_skills: List[str]
    recommendation: str

class CVAnalysisResponse(BaseModel):
    skills_detected: List[str]
    experience_level: str
    years_of_experience: int
    education_detected: List[str]
    languages_detected: List[str]
    summary: str

class RecommendationRequest(BaseModel):
    candidate_skills: List[str]
    previous_applications: List[int]
    preferred_domain: Optional[str] = ""
    years_of_experience: Optional[int] = 0

class RecommendationResponse(BaseModel):
    recommended_jobs: List[int]
    reasoning: str

class ScoringRequest(BaseModel):
    years_of_experience: int
    number_of_skills: int
    certifications_count: int
    education_level: Optional[str] = "BACHELOR"
    has_portfolio: Optional[bool] = False

class ScoringResponse(BaseModel):
    candidate_score: int
    breakdown: dict
    level: str
    feedback: str


class InterviewWhatsAppRequest(BaseModel):
    to_phone: str
    candidate_name: str
    job_title: str
    company_name: str
    interview_date: str
    interview_time: str
    interview_type: str
    location: Optional[str] = ""


# =============================================
# CONSTANTS & HELPERS
# =============================================

# Expanded skills database grouped by domain
SKILL_DATABASE = {
    "backend": ["Java", "Spring Boot", "Spring", "Python", "Django", "Flask", "FastAPI", "Node.js",
                "PHP", "Laravel", "Symfony", "C#", ".NET", "ASP.NET", "Go", "Rust", "Ruby", "Rails",
                "Kotlin", "Scala", "Hibernate", "JPA", "REST API", "GraphQL", "gRPC"],
    "frontend": ["JavaScript", "TypeScript", "React", "Angular", "Vue", "Vue.js", "Svelte",
                 "HTML", "CSS", "Tailwind", "Bootstrap", "Sass", "SCSS", "jQuery", "Next.js",
                 "Nuxt.js", "Redux", "webpack", "Vite", "Figma", "UX", "UI"],
    "mobile": ["Android", "Swift", "Kotlin", "Flutter", "React Native", "Ionic", "Xamarin",
               "iOS", "Objective-C"],
    "database": ["MySQL", "PostgreSQL", "MongoDB", "Redis", "Elasticsearch", "Oracle", "SQLite",
                 "MariaDB", "Cassandra", "DynamoDB", "SQL", "NoSQL", "PL/SQL", "Firebase"],
    "devops": ["Docker", "Kubernetes", "AWS", "Azure", "GCP", "Google Cloud", "Jenkins",
               "GitLab CI", "GitHub Actions", "Terraform", "Ansible", "Linux", "Nginx",
               "Apache", "CI/CD", "Helm", "Prometheus", "Grafana"],
    "data": ["Machine Learning", "Deep Learning", "AI", "TensorFlow", "PyTorch", "scikit-learn",
             "Pandas", "NumPy", "R", "Data Science", "Big Data", "Spark", "Hadoop", "Power BI",
             "Tableau", "Matplotlib"],
    "tools": ["Git", "GitHub", "Jira", "Confluence", "Postman", "Swagger", "IntelliJ", "VS Code",
              "Maven", "Gradle", "npm", "pip", "Linux", "Bash", "PowerShell"],
    "soft": ["Agile", "Scrum", "Kanban", "Communication", "Leadership", "Problem Solving",
             "Team Work"]
}

ALL_SKILLS = []
for domain_skills in SKILL_DATABASE.values():
    ALL_SKILLS.extend(domain_skills)

EDUCATION_KEYWORDS = [
    "Licence", "Bachelor", "Master", "Mastère", "Ingénieur", "Doctorat", "PhD", "MBA",
    "BTS", "DUT", "ESPRIT", "FST", "ENSI", "INSAT", "SUP'COM", "ISET", "Université",
    "Baccalauréat", "École", "Institut", "Engineer", "Computer Science", "Informatique"
]

LANGUAGE_KEYWORDS = [
    "Français", "Anglais", "Arabic", "Arabe", "English", "French", "Spanish", "Espagnol",
    "Allemand", "German", "Italien", "Italian"
]

EXPERIENCE_LEVEL_MAP = {
    "JUNIOR": (0, 2),
    "INTERMEDIATE": (2, 5),
    "SENIOR": (5, 99)
}


def normalize_skill(skill: str) -> str:
    return skill.strip().lower()


def detect_skills_advanced(text: str) -> List[str]:
    """Advanced skill detection with fuzzy matching."""
    detected = []
    text_lower = text.lower()
    for skill in ALL_SKILLS:
        # Use word boundary for exact matching
        pattern = rf'\b{re.escape(skill.lower())}\b'
        if re.search(pattern, text_lower):
            detected.append(skill)
    # Deduplicate
    seen = set()
    result = []
    for s in detected:
        if s.lower() not in seen:
            seen.add(s.lower())
            result.append(s)
    return result


def extract_years_of_experience(text: str) -> int:
    """Extract total years of experience from CV text."""
    patterns = [
        r'(\d+)\s*(?:\+\s*)?(?:years?|ans?|années?)\s*(?:of\s+)?(?:experience|d\'expérience|expérience)',
        r'(?:experience|expérience)\s*(?:of\s*)?:?\s*(\d+)\s*(?:years?|ans?)',
        r'(\d{4})\s*[-–]\s*(\d{4}|\bpresent\b|\bactuel\b|\bactuelle\b)',
    ]
    max_years = 0
    for pattern in patterns:
        matches = re.findall(pattern, text, re.IGNORECASE)
        for match in matches:
            if isinstance(match, tuple) and len(match) == 2:
                # Date range
                try:
                    start = int(match[0])
                    end_str = str(match[1]).lower()
                    if end_str in ['present', 'actuel', 'actuelle']:
                        end = 2026
                    else:
                        end = int(match[1])
                    years = max(0, end - start)
                    max_years = max(max_years, years)
                except ValueError:
                    pass
            else:
                try:
                    years = int(match[0] if isinstance(match, tuple) else match)
                    if 0 < years < 50:
                        max_years = max(max_years, years)
                except ValueError:
                    pass
    return min(max_years, 40)  # Cap at 40 years


def estimate_experience_level(years: int) -> str:
    if years >= 5:
        return "Senior"
    elif years >= 2:
        return "Intermediate"
    return "Junior"


def detect_education(text: str) -> List[str]:
    found = []
    for kw in EDUCATION_KEYWORDS:
        if re.search(rf'\b{re.escape(kw)}\b', text, re.IGNORECASE):
            if kw not in found:
                found.append(kw)
    return found[:5]


def detect_languages(text: str) -> List[str]:
    found = []
    for lang in LANGUAGE_KEYWORDS:
        if re.search(rf'\b{re.escape(lang)}\b', text, re.IGNORECASE):
            if lang not in found:
                found.append(lang)
    return found


def compute_skill_match(candidate_skills: List[str], job_skills: List[str]) -> dict:
    """
    Multi-factor skill matching:
    1. Exact keyword overlap
    2. TF-IDF cosine similarity on combined text
    """
    if not job_skills:
        return {"percentage": 0, "matched": [], "missing": []}

    cand_norm = [normalize_skill(s) for s in candidate_skills]
    job_norm = [normalize_skill(s) for s in job_skills]

    matched = [s for s in job_skills if normalize_skill(s) in cand_norm]
    missing = [s for s in job_skills if normalize_skill(s) not in cand_norm]

    keyword_score = len(matched) / len(job_norm) if job_norm else 0

    # TF-IDF similarity on combined text
    tfidf_score = 0.0
    if candidate_skills and job_skills:
        try:
            cand_text = " ".join(candidate_skills)
            job_text = " ".join(job_skills)
            vectorizer = TfidfVectorizer(ngram_range=(1, 2), analyzer='word')
            tfidf_matrix = vectorizer.fit_transform([cand_text, job_text])
            sim = cosine_similarity(tfidf_matrix[0:1], tfidf_matrix[1:2])[0][0]
            tfidf_score = float(sim)
        except Exception:
            tfidf_score = keyword_score

    # Weighted combination: 70% keyword, 30% TF-IDF
    final_score = (keyword_score * 0.70) + (tfidf_score * 0.30)
    percentage = min(100, int(final_score * 100))

    return {
        "percentage": percentage,
        "matched": matched,
        "missing": missing
    }


def generate_match_recommendation(percentage: int, missing: List[str]) -> str:
    if percentage >= 85:
        return f"Excellent candidat. Correspondance très haute ({percentage}%). Recommandé pour entretien."
    elif percentage >= 65:
        miss = ", ".join(missing[:3]) if missing else "aucune"
        return f"Bon candidat ({percentage}%). Compétences manquantes: {miss}. Peut être formé."
    elif percentage >= 40:
        miss = ", ".join(missing[:3]) if missing else "aucune"
        return f"Correspondance partielle ({percentage}%). Lacunes importantes: {miss}."
    else:
        return f"Faible correspondance ({percentage}%). Profil non adapté à ce poste."


def format_phone_e164(phone: str) -> str:
    """Format phone to E.164 for Twilio WhatsApp (e.g. +21612345678)."""
    if not phone:
        return phone or ""
    digits = "".join(c for c in phone if c.isdigit())
    if phone.strip().startswith("+"):
        return "+" + digits if digits else phone
    if len(digits) == 8:
        return f"+216{digits}"
    if len(digits) == 9 and digits.startswith("2"):
        return f"+216{digits[1:]}"
    if len(digits) >= 10 and digits.startswith("216"):
        return f"+{digits[:12]}"
    if digits:
        return f"+216{digits}"
    return phone


def send_whatsapp(to_phone: str, body: str) -> dict:
    """Send WhatsApp message via Twilio. Returns {status, message, ...}."""
    account_sid = os.getenv("TWILIO_ACCOUNT_SID", "").strip()
    auth_token = os.getenv("TWILIO_AUTH_TOKEN", "").strip()
    from_whatsapp = os.getenv("TWILIO_WHATSAPP_NUMBER", "").strip()  # e.g. +14155238886 (sandbox)

    if not account_sid or not auth_token or "your_" in account_sid.lower() or "your_" in auth_token.lower():
        print(f"[WhatsApp MOCK] To: {to_phone} | Message: {body[:80]}...")
        return {"status": "mock_success", "message": "Twilio not configured – WhatsApp logged only."}

    if not from_whatsapp:
        print(f"[WhatsApp ERROR] TWILIO_WHATSAPP_NUMBER not set")
        return {"status": "error", "message": "TWILIO_WHATSAPP_NUMBER not set in .env"}

    to_formatted = format_phone_e164(to_phone)
    from_w = from_whatsapp if from_whatsapp.startswith("whatsapp:") else f"whatsapp:{format_phone_e164(from_whatsapp)}"
    to_w = to_formatted if to_formatted.startswith("whatsapp:") else f"whatsapp:{to_formatted}"

    try:
        client = Client(account_sid, auth_token)
        msg = client.messages.create(body=body, from_=from_w, to=to_w)
        print(f"[WhatsApp SENT] To: {to_w} SID: {msg.sid}")
        return {"status": "success", "message_sid": msg.sid}
    except Exception as e:
        print(f"[WhatsApp ERROR] {e}")
        return {"status": "error", "message": str(e)}


# =============================================
# ENDPOINTS
# =============================================

@app.get("/", tags=["Health"])
async def root():
    return {
        "api": "Skilora AI Recruitment API",
        "version": "2.0.0",
        "status": "operational",
        "endpoints": ["/match", "/analyze-cv", "/recommend-jobs", "/calculate-score", "/send-interview-whatsapp"]
    }

@app.get("/health", tags=["Health"])
async def health():
    return {"status": "ok"}


# ---- SMART MATCHING ----

@app.post("/match", response_model=MatchResponse, tags=["AI"])
async def match_skills(request: MatchRequest):
    """
    Smart skill matching between candidate and job offer.
    Uses TF-IDF + keyword overlap for accurate scoring.
    """
    result = compute_skill_match(request.candidate_skills, request.job_skills)

    # Bonus for experience level match
    required_level = (request.required_experience_level or "JUNIOR").upper()
    exp_range = EXPERIENCE_LEVEL_MAP.get(required_level, (0, 2))
    exp_years = request.candidate_experience_years or 0
    exp_bonus = 5 if exp_range[0] <= exp_years <= exp_range[1] + 2 else 0

    final_pct = min(100, result["percentage"] + exp_bonus)
    recommendation = generate_match_recommendation(final_pct, result["missing"])

    return MatchResponse(
        match_percentage=final_pct,
        matched_skills=result["matched"],
        missing_skills=result["missing"],
        recommendation=recommendation
    )


# ---- CV ANALYSIS ----

@app.post("/analyze-cv", response_model=CVAnalysisResponse, tags=["AI"])
async def analyze_cv(file: UploadFile = File(...)):
    """
    Analyze a PDF CV to extract:
    - Skills detected (from our comprehensive database)
    - Experience level and years
    - Education keywords
    - Languages
    """
    if not file.filename.lower().endswith(".pdf"):
        raise HTTPException(status_code=400, detail="Seuls les fichiers PDF sont acceptés.")

    try:
        content = await file.read()
        pdf_reader = PyPDF2.PdfReader(io.BytesIO(content))
        text = ""
        for page in pdf_reader.pages:
            page_text = page.extract_text()
            if page_text:
                text += page_text + "\n"
    except Exception as e:
        raise HTTPException(status_code=422, detail=f"Impossible de lire le PDF: {str(e)}")

    skills = detect_skills_advanced(text)
    years = extract_years_of_experience(text)
    level = estimate_experience_level(years)
    education = detect_education(text)
    languages = detect_languages(text)

    summary_parts = []
    if skills:
        summary_parts.append(f"{len(skills)} compétences détectées")
    if years > 0:
        summary_parts.append(f"{years} ans d'expérience")
    summary_parts.append(f"Niveau: {level}")
    summary = " · ".join(summary_parts) if summary_parts else "Analyse complète du CV."

    return CVAnalysisResponse(
        skills_detected=skills,
        experience_level=level,
        years_of_experience=years,
        education_detected=education,
        languages_detected=languages,
        summary=summary
    )


# ---- RECOMMENDATIONS ----

@app.post("/recommend-jobs", response_model=RecommendationResponse, tags=["AI"])
async def recommend_jobs(request: RecommendationRequest):
    """
    Recommend job IDs based on candidate skills and domain preference.
    Uses a domain-skill heuristic to score relevance.
    """
    candidate_skills_lower = [s.lower() for s in request.candidate_skills]
    domain = (request.preferred_domain or "").lower()

    # Domain → relevant skill keywords mapping
    domain_skill_map = {
        "backend":   ["java", "spring", "python", "django", "node.js", "php", "c#", ".net"],
        "frontend":  ["react", "angular", "vue", "javascript", "typescript", "css", "html"],
        "fullstack": ["react", "node.js", "java", "python", "javascript", "mysql"],
        "devops":    ["docker", "kubernetes", "aws", "azure", "jenkins", "linux", "terraform"],
        "data":      ["python", "machine learning", "ai", "tensorflow", "pandas", "sql", "r"],
        "mobile":    ["android", "swift", "kotlin", "flutter", "react native", "ios"],
    }

    # Score each domain
    domain_scores = {}
    for d, d_skills in domain_skill_map.items():
        overlap = sum(1 for s in d_skills if s in candidate_skills_lower)
        domain_scores[d] = overlap

    best_domain = max(domain_scores, key=domain_scores.get) if domain_scores else "backend"

    # Map domains to job offer IDs (static mock based on our sample data)
    domain_job_map = {
        "backend":   [1, 4],
        "frontend":  [2, 4],
        "fullstack": [4, 1, 2],
        "devops":    [3, 1],
        "data":      [3, 4],
        "mobile":    [2, 4],
    }

    preferred_jobs = domain_job_map.get(domain, domain_job_map.get(best_domain, [1, 2, 3]))

    # Exclude already applied jobs
    previous = set(request.previous_applications or [])
    filtered = [jid for jid in preferred_jobs if jid not in previous]

    # Add general recommendations if filtered list is short
    all_jobs = [1, 2, 3, 4]
    for jid in all_jobs:
        if jid not in filtered and jid not in previous and len(filtered) < 5:
            filtered.append(jid)

    reasoning = f"Recommandations basées sur vos compétences en {best_domain}. Domaine préféré: {best_domain}."
    if request.previous_applications:
        reasoning += f" Offres déjà postulées exclues ({len(request.previous_applications)})."

    return RecommendationResponse(
        recommended_jobs=filtered[:5],
        reasoning=reasoning
    )


# ---- CANDIDATE SCORING ----

@app.post("/calculate-score", response_model=ScoringResponse, tags=["AI"])
async def calculate_score(request: ScoringRequest):
    """
    Score a candidate on a 100-point scale:
    - Experience: up to 35 points
    - Skills: up to 30 points
    - Certifications: up to 15 points
    - Education: up to 15 points
    - Portfolio: up to 5 points
    """
    # Experience score (log-scaled for diminishing returns)
    exp_years = max(0, request.years_of_experience)
    exp_score = int(min(35, 35 * (1 - math.exp(-exp_years / 8))))

    # Skills score
    skills_score = int(min(30, request.number_of_skills * 2.5))

    # Certifications score
    cert_score = int(min(15, request.certifications_count * 5))

    # Education score
    education_map = {
        "PHD": 15, "MASTER": 12, "ENGINEER": 12, "BACHELOR": 9, "BTS": 6, "SELF_TAUGHT": 5
    }
    edu_score = education_map.get((request.education_level or "BACHELOR").upper(), 9)

    # Portfolio bonus
    portfolio_score = 5 if request.has_portfolio else 0

    total = exp_score + skills_score + cert_score + edu_score + portfolio_score
    total = min(100, total)

    # Level classification
    if total >= 80:
        level = "Expert"
        feedback = "Profil d'élite. Candidat très compétitif sur le marché."
    elif total >= 60:
        level = "Senior"
        feedback = "Excellent profil. Candidat expérimenté très recherché."
    elif total >= 40:
        level = "Intermediate"
        feedback = "Bon profil en développement. Potentiel d'évolution important."
    elif total >= 20:
        level = "Junior"
        feedback = "Profil débutant. À accompagner et former."
    else:
        level = "Entry"
        feedback = "Profil en début de parcours. Potentiel à développer."

    return ScoringResponse(
        candidate_score=total,
        breakdown={
            "experience": exp_score,
            "skills": skills_score,
            "certifications": cert_score,
            "education": edu_score,
            "portfolio": portfolio_score
        },
        level=level,
        feedback=feedback
    )


# ---- WhatsApp: interview scheduled ----

@app.post("/send-interview-whatsapp", tags=["WhatsApp"])
async def send_interview_whatsapp(request: InterviewWhatsAppRequest):
    """
    Send a WhatsApp message to the candidate when an interview is scheduled.
    Called automatically by the app after saving the interview.
    """
    interview_type_lower = (request.interview_type or "").lower()
    is_online = "online" in interview_type_lower or "en ligne" in interview_type_lower or "video" in interview_type_lower
    type_text = "En ligne (Online)" if is_online else "En présentiel (Onsite)"
    location_line = f"\n📍 Lieu: {request.location}" if request.location and not is_online else ""

    body = (
        f"🎯 *Skilora* – Entretien planifié\n\n"
        f"Bonjour {request.candidate_name},\n\n"
        f"Votre entretien pour le poste *{request.job_title}* chez *{request.company_name}* a été planifié.\n\n"
        f"📅 Date: {request.interview_date}\n"
        f"🕐 Heure: {request.interview_time}\n"
        f"💼 Type: {type_text}"
        f"{location_line}\n\n"
        f"Merci de vous préparer. Bonne chance ! 🌟\n"
        f"— L'équipe Skilora"
    )
    result = send_whatsapp(request.to_phone, body)
    return result


if __name__ == "__main__":
    import uvicorn
    print("🚀 Starting Skilora AI API v2.0.0 on http://localhost:8000")
    print("📖 Docs: http://localhost:8000/docs")
    uvicorn.run(app, host="0.0.0.0", port=8000, reload=False)
