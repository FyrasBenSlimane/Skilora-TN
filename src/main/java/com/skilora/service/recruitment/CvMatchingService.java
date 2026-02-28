package com.skilora.service.recruitment;

import com.skilora.model.entity.recruitment.JobOffer;
import com.skilora.model.entity.usermanagement.Experience;
import com.skilora.model.entity.usermanagement.Skill;
import com.skilora.service.usermanagement.ProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * CvMatchingService
 *
 * Pure-Java matching engine that compares a candidate's profile (skills +
 * experience) with a job offer (required skills + description keywords) and
 * returns a 0–100 compatibility score together with a breakdown of matched /
 * missing skills.
 *
 * Algorithm overview
 * ──────────────────
 *  1. Build a "candidate corpus": all skill names + experience titles +
 *     experience descriptions, normalised to lowercase tokens.
 *  2. Collect "job terms": (a) the required-skills list from the offer
 *     (weight 70 %) and (b) meaningful tech keywords extracted from the
 *     description (weight 30 %).
 *  3. For each required skill check direct, alias, and partial matches.
 *  4. Final score  =  skill_portion × 0.70  +  desc_portion × 0.30
 *     capped at 100.
 *
 * No external dependencies – uses only the existing ProfileService and
 * JobService singletons.
 */
public class CvMatchingService {

    private static final Logger logger = LoggerFactory.getLogger(CvMatchingService.class);
    private static volatile CvMatchingService instance;

    private final ProfileService profileService = ProfileService.getInstance();
    private final JobService     jobService     = JobService.getInstance();

    // ── Synonym / alias table ────────────────────────────────────────────────
    // Key = canonical lowercase term.  Values = alternate spellings the
    // candidate might use.  Matching is bidirectional.
    private static final Map<String, Set<String>> SYNONYMS;
    static {
        Map<String, Set<String>> m = new HashMap<>();
        m.put("javascript",      set("js", "ecmascript", "es6", "es2015", "es2016", "es2017", "es2018", "es2019", "es2020", "es2021"));
        m.put("typescript",      set("ts"));
        m.put("python",          set("py", "python3", "python2"));
        m.put("java",            set("jvm", "java8", "java11", "java17", "java21"));
        m.put("spring boot",     set("spring", "springframework", "springboot", "spring mvc", "spring framework"));
        m.put("react",           set("reactjs", "react.js", "react native", "reactnative"));
        m.put("angular",         set("angularjs", "angular.js", "ng"));
        m.put("vue",             set("vuejs", "vue.js", "vue3", "vue2", "nuxt", "nuxtjs"));
        m.put("node.js",         set("nodejs", "node", "express", "expressjs"));
        m.put("mongodb",         set("mongo", "nosql", "document db"));
        m.put("postgresql",      set("postgres", "psql", "pgsql"));
        m.put("mysql",           set("mariadb", "sql", "rdbms"));
        m.put("sql",             set("plsql", "tsql", "sqlite", "database"));
        m.put("docker",          set("containerization", "container", "dockerfile"));
        m.put("kubernetes",      set("k8s", "helm", "kubectl", "orchestration"));
        m.put("git",             set("github", "gitlab", "bitbucket", "version control", "vcs"));
        m.put("rest api",        set("rest", "restful", "http api", "api", "web service", "webservice"));
        m.put("graphql",         set("gql"));
        m.put("machine learning",set("ml", "deep learning", "neural network", "ai", "artificial intelligence", "sklearn", "tensorflow", "pytorch", "keras"));
        m.put("devops",          set("ci/cd", "cicd", "jenkins", "github actions", "gitlab ci", "teamcity", "circle ci"));
        m.put("linux",           set("unix", "bash", "shell", "ubuntu", "debian", "centos"));
        m.put("c#",              set("csharp", ".net", "dotnet", "asp.net", "aspnet"));
        m.put("php",             set("laravel", "symfony", "composer"));
        m.put("html",            set("html5", "markup"));
        m.put("css",             set("css3", "sass", "scss", "less", "tailwind", "bootstrap"));
        m.put("aws",             set("amazon web services", "ec2", "s3", "lambda", "rds", "cloudformation"));
        m.put("azure",           set("microsoft azure", "azure devops"));
        m.put("gcp",             set("google cloud", "google cloud platform", "bigquery", "cloud run"));
        m.put("android",         set("kotlin", "android studio", "android sdk"));
        m.put("ios",             set("swift", "objective-c", "xcode", "swiftui"));
        m.put("flutter",         set("dart"));
        m.put("redux",           set("ngrx", "vuex", "state management"));
        m.put("hibernate",       set("jpa", "orm", "entity framework"));
        m.put("microservices",   set("micro services", "service mesh", "istio"));
        m.put("agile",           set("scrum", "kanban", "sprint", "jira", "confluence"));
        SYNONYMS = Collections.unmodifiableMap(m);
    }

    // Tokens that carry no technical meaning
    private static final Set<String> STOP_WORDS = set(
        "the", "and", "or", "in", "on", "at", "of", "to", "a", "an", "is", "are",
        "was", "were", "be", "been", "being", "have", "has", "had", "do", "does",
        "did", "will", "would", "could", "should", "may", "might", "shall", "can",
        "for", "with", "as", "by", "from", "that", "this", "it", "we", "you", "he",
        "she", "they", "our", "your", "their", "its", "not", "no", "but", "if",
        "so", "up", "out", "about", "more", "also", "than", "any", "all", "both",
        "each", "few", "how", "when", "where", "who", "which", "what", "why",
        "team", "work", "working", "environment", "good", "great", "strong",
        "using", "use", "used", "including", "include", "experience", "years",
        "knowledge", "ability", "skills", "skill", "excellent", "proficient",
        "familiar", "proficiency", "understanding", "ability", "required",
        "preferred", "plus", "bonus", "nice", "must", "need"
    );

    private CvMatchingService() {}

    public static CvMatchingService getInstance() {
        if (instance == null) {
            synchronized (CvMatchingService.class) {
                if (instance == null) instance = new CvMatchingService();
            }
        }
        return instance;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Full result with score + breakdown (matched / missing skills).
     */
    public static class MatchResult {
        public final int          score;
        public final List<String> matchedSkills;
        public final List<String> missingSkills;
        public final int          totalRequired;

        private MatchResult(int score, List<String> matched, List<String> missing, int total) {
            this.score         = score;
            this.matchedSkills = Collections.unmodifiableList(matched);
            this.missingSkills = Collections.unmodifiableList(missing);
            this.totalRequired = total;
        }

        public static final MatchResult EMPTY =
                new MatchResult(0, new ArrayList<>(), new ArrayList<>(), 0);

        /** Human-readable label for the score. */
        public String label() {
            if (score >= 85) return "Excellent";
            if (score >= 70) return "Très bon";
            if (score >= 50) return "Bon";
            if (score >= 30) return "Partiel";
            return "Faible";
        }

        /** CSS-friendly colour token based on the score. */
        public String colorHex() {
            if (score >= 85) return "#16a34a";  // green
            if (score >= 70) return "#2563eb";  // blue
            if (score >= 50) return "#d97706";  // amber
            if (score >= 30) return "#ea580c";  // orange
            return "#dc2626";                   // red
        }
    }

    /**
     * Computes the compatibility score between a candidate profile and a job
     * offer.  Returns {@link MatchResult#EMPTY} when either ID is invalid or
     * data cannot be loaded.
     */
    public MatchResult calculate(int profileId, int jobOfferId) {
        try {
            // ── 1. Load candidate corpus ────────────────────────────────────
            List<Skill>      skills  = profileService.findSkillsByProfileId(profileId);
            List<Experience> exps    = profileService.findExperiencesByProfileId(profileId);

            Set<String> candidateTokens = buildCandidateCorpus(skills, exps);
            if (candidateTokens.isEmpty()) {
                return MatchResult.EMPTY;
            }

            // ── 2. Load job offer ───────────────────────────────────────────
            JobOffer offer = jobService.findJobOfferById(jobOfferId);
            if (offer == null) return MatchResult.EMPTY;

            // ── 3. Match required skills (70 %) ────────────────────────────
            List<String> required = normaliseList(offer.getRequiredSkills());
            List<String> matched  = new ArrayList<>();
            List<String> missing  = new ArrayList<>();

            for (String req : required) {
                if (skillPresent(req, candidateTokens)) {
                    matched.add(req);
                } else {
                    missing.add(req);
                }
            }

            double skillPortion = required.isEmpty() ? 1.0
                    : (double) matched.size() / required.size();

            // ── 4. Match description keywords (30 %) ───────────────────────
            List<String> descKeywords = extractTechKeywords(offer.getDescription(), offer.getTitle());
            int descMatched = 0;
            for (String kw : descKeywords) {
                if (skillPresent(kw, candidateTokens)) descMatched++;
            }
            double descPortion = descKeywords.isEmpty() ? skillPortion
                    : (double) descMatched / descKeywords.size();

            // ── 5. Final score ──────────────────────────────────────────────
            double raw   = skillPortion * 70.0 + descPortion * 30.0;
            int    score = (int) Math.min(100, Math.round(raw));

            logger.debug("Match profile={} job={} → {}/{} skills  descKw={}/{} → {}%",
                    profileId, jobOfferId,
                    matched.size(), required.size(),
                    descMatched, descKeywords.size(), score);

            return new MatchResult(score, matched, missing, required.size());

        } catch (Exception e) {
            logger.warn("CvMatchingService.calculate({}, {}) failed: {}", profileId, jobOfferId, e.getMessage());
            return MatchResult.EMPTY;
        }
    }

    /**
     * Convenience method – returns just the integer score.
     */
    public int calculateScore(int profileId, int jobOfferId) {
        return calculate(profileId, jobOfferId).score;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Batch-friendly API (no DB queries – use pre-loaded data)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds a reusable candidate corpus from pre-loaded skills and
     * experiences (zero DB queries).  Call once, then reuse across many jobs.
     */
    public Set<String> buildCorpus(List<Skill> skills, List<Experience> experiences) {
        return buildCandidateCorpus(skills, experiences);
    }

    /**
     * Scores a job using a pre-built corpus and the job's own fields
     * (requiredSkills list + description text).  Zero DB queries.
     * Use this in batch scenarios (feed page) to avoid N×3 DB round-trips.
     */
    public int scoreFromCorpus(Set<String> corpus,
                               List<String> requiredSkills,
                               String description,
                               String title) {
        if (corpus == null || corpus.isEmpty()) return 0;

        List<String> required = normaliseList(requiredSkills);
        int matchedCount = 0;
        for (String req : required) {
            if (skillPresent(req, corpus)) matchedCount++;
        }
        double skillPortion = required.isEmpty() ? 1.0
                : (double) matchedCount / required.size();

        List<String> descKw = extractTechKeywords(description, title);
        int descMatched = 0;
        for (String kw : descKw) {
            if (skillPresent(kw, corpus)) descMatched++;
        }
        double descPortion = descKw.isEmpty() ? skillPortion
                : (double) descMatched / descKw.size();

        return (int) Math.min(100, Math.round(skillPortion * 70.0 + descPortion * 30.0));
    }

    /**
     * Computes an overall "candidate quality" score (0–100) based on profile
     * completeness, skills count, and work experience.
     */
    public int scoreCandidateProfile(int profileId) {
        try {
            var profile = profileService.findProfileById(profileId);
            if (profile == null) return 0;

            List<Skill>      skills = profileService.findSkillsByProfileId(profileId);
            List<Experience> exps   = profileService.findExperiencesByProfileId(profileId);

            int score = 0;

            // Profile completeness (up to 25 pts)
            if (!blank(profile.getFirstName()))  score += 5;
            if (!blank(profile.getLastName()))   score += 5;
            if (!blank(profile.getPhone()))      score += 5;
            if (!blank(profile.getLocation()))   score += 5;
            if (profile.getBirthDate() != null)  score += 5;

            // Skills (up to 35 pts – 3.5 pts each, capped at 10)
            int skillPts = Math.min(skills.size(), 10);
            score += skillPts * 3 + (skills.size() >= 5 ? 5 : 0);

            // Experience (up to 30 pts)
            if (!exps.isEmpty()) score += 15;
            if (exps.size() >= 2) score += 10;
            if (exps.size() >= 3) score += 5;

            // CV uploaded (10 pts)
            if (!blank(profile.getCvUrl())) score += 10;

            return Math.min(100, score);
        } catch (Exception e) {
            logger.warn("scoreCandidateProfile({}) failed: {}", profileId, e.getMessage());
            return 0;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Core helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds the normalised token set that represents "what the candidate knows".
     * Includes skill names, all their known synonyms/aliases, and tokenised
     * experience text.
     */
    private Set<String> buildCandidateCorpus(List<Skill> skills, List<Experience> exps) {
        Set<String> corpus = new HashSet<>();

        // Skill names + their canonical forms + their aliases
        for (Skill s : skills) {
            String norm = normalise(s.getSkillName());
            if (!norm.isEmpty()) {
                corpus.add(norm);
                expandAliases(norm, corpus);
            }
        }

        // Experience: position titles + descriptions
        for (Experience e : exps) {
            tokenise(e.getPosition(),    corpus);
            tokenise(e.getDescription(), corpus);
        }

        return corpus;
    }

    /**
     * Returns true if the required skill (or any of its aliases) appears in
     * the candidate corpus.  Supports:
     *  - exact match
     *  - contains match  ("spring boot" found when corpus has "spring")
     *  - alias/synonym expansion
     *  - partial token match (first 4+ chars)
     */
    private boolean skillPresent(String required, Set<String> corpus) {
        String norm = normalise(required);
        if (norm.isEmpty()) return false;

        // 1. Direct
        if (corpus.contains(norm)) return true;

        // 2. Corpus token contains or is contained in required
        for (String token : corpus) {
            if (token.contains(norm) || norm.contains(token)) return true;
        }

        // 3. Alias expansion of the required term
        Set<String> reqAliases = aliasesOf(norm);
        for (String alias : reqAliases) {
            if (corpus.contains(alias)) return true;
            for (String token : corpus) {
                if (token.contains(alias) || alias.contains(token)) return true;
            }
        }

        // 4. Alias expansion of every token in corpus to see if it covers required
        for (String token : corpus) {
            Set<String> tokenAliases = aliasesOf(token);
            if (tokenAliases.contains(norm)) return true;
            for (String ta : tokenAliases) {
                if (ta.contains(norm) || norm.contains(ta)) return true;
            }
        }

        // 5. Partial match – first 5 chars of multi-char tokens
        if (norm.length() >= 5) {
            String prefix = norm.substring(0, 5);
            for (String token : corpus) {
                if (token.length() >= 5 && token.startsWith(prefix)) return true;
            }
        }

        return false;
    }

    /**
     * Extracts meaningful technical / domain keywords from free-text fields.
     * Skips stop-words and very short tokens.  Returns at most 25 unique terms.
     */
    private List<String> extractTechKeywords(String description, String title) {
        Set<String> keywords = new LinkedHashSet<>();

        // Prioritise title tokens
        tokenise(title, keywords);

        // Then description
        if (description != null) {
            String[] rawTokens = description.toLowerCase().split("[^a-z0-9#+.\\-/]");
            for (String tok : rawTokens) {
                tok = tok.trim();
                if (tok.length() >= 2 && tok.length() <= 30
                        && !STOP_WORDS.contains(tok)
                        && !tok.matches("\\d+")) {
                    keywords.add(tok);
                    if (keywords.size() >= 40) break;
                }
            }
        }

        // Expand each keyword into its canonical form
        Set<String> expanded = new LinkedHashSet<>();
        for (String kw : keywords) {
            expanded.add(kw);
            expandAliases(kw, expanded);
        }

        return expanded.stream().limit(25).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Low-level utilities
    // ─────────────────────────────────────────────────────────────────────────

    private static String normalise(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.ROOT).trim()
                .replaceAll("[^a-z0-9#+.\\-/]", " ")
                .replaceAll("\\s+", " ").trim();
    }

    /** Tokenise free text and add non-stop-word tokens ≥ 2 chars to the set. */
    private static void tokenise(String text, Set<String> out) {
        if (text == null || text.isEmpty()) return;
        for (String tok : text.toLowerCase(Locale.ROOT).split("[^a-z0-9#+.\\-/]")) {
            tok = tok.trim();
            if (tok.length() >= 2 && !STOP_WORDS.contains(tok) && !tok.matches("\\d+")) {
                out.add(tok);
            }
        }
    }

    /** Expand a normalised term into all known aliases and add them to the set. */
    private static void expandAliases(String term, Set<String> out) {
        // term → aliases
        Set<String> direct = SYNONYMS.get(term);
        if (direct != null) out.addAll(direct);

        // aliases → canonical (reverse lookup)
        for (Map.Entry<String, Set<String>> entry : SYNONYMS.entrySet()) {
            if (entry.getValue().contains(term)) {
                out.add(entry.getKey());
                out.addAll(entry.getValue());
            }
        }
    }

    /** All aliases of a term (bidirectional lookup). */
    private static Set<String> aliasesOf(String term) {
        Set<String> result = new HashSet<>();
        expandAliases(term, result);
        return result;
    }

    private static List<String> normaliseList(List<String> raw) {
        if (raw == null) return new ArrayList<>();
        return raw.stream()
                .map(CvMatchingService::normalise)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    @SafeVarargs
    private static <T> Set<T> set(T... items) {
        Set<T> s = new HashSet<>();
        Collections.addAll(s, items);
        return s;
    }
}
