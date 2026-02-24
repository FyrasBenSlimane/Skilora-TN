package com.skilora.community.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * TranslationService — Service d'intégration de l'API de traduction MyMemory.
 *
 * ╔═══════════════════════════════════════════════════════════════════╗
 * ║  API UTILISÉE : MyMemory Translated (https://mymemory.translated.net) ║
 * ║  Type        : API REST gratuite (pas de clé API requise)       ║
 * ║  Format      : JSON                                              ║
 * ║  Limite      : 5000 caractères par requête, 10 000/jour gratuit ║
 * ╚═══════════════════════════════════════════════════════════════════╝
 *
 * Stratégie de traduction (multi-couche) :
 *   1. Dictionnaire local intégré — résultats instantanés et fiables
 *   2. API MyMemory avec filtre qualité — traduction machine (&mt=1)
 *   3. Traduction en 2 étapes (fr→en→ar) — contourne les paires faibles
 *   4. Validation stricte — rejette les résultats garbage
 *
 * Langues supportées (pertinentes pour Skilora Tunisie) :
 *   - "fr" → Français
 *   - "en" → Anglais
 *   - "ar" → Arabe
 *
 * Pattern : Singleton thread-safe (cohérent avec les autres services)
 *
 * Utilisation dans le contrôleur :
 *   String traduit = TranslationService.getInstance().translate("Hello", "en", "fr");
 *   // Résultat : "Bonjour"
 */
public class TranslationService {

    private static final Logger logger = LoggerFactory.getLogger(TranslationService.class);

    // URL de base de l'API MyMemory (gratuite, sans clé)
    private static final String API_URL = "https://api.mymemory.translated.net/get";

    // ── Singleton (instance unique) ──
    private static volatile TranslationService instance;

    /**
     * Cache LRU (Least Recently Used) pour éviter les appels API redondants.
     * Clé : "texte|langSource|langCible"
     * Valeur : texte traduit
     * Capacité maximale : 100 entrées (les plus anciennes sont supprimées)
     */
    private final Map<String, String> cache = new LinkedHashMap<>(100, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > 100; // Supprimer l'entrée la plus ancienne si > 100
        }
    };

    /**
     * Dictionnaire local intégré pour les mots/phrases courants.
     * Clé : "texteMinuscule|langSource|langCible"
     * Valeur : traduction fiable
     * Sert de première couche avant l'appel API (résultats instantanés et garantis).
     */
    private static final Map<String, String> LOCAL_DICT = new HashMap<>();
    static {
        // ── Français → Arabe ──
        dict("bonjour", "fr", "ar", "مرحبا");
        dict("bonsoir", "fr", "ar", "مساء الخير");
        dict("salut", "fr", "ar", "أهلاً");
        dict("merci", "fr", "ar", "شكراً");
        dict("bienvenue", "fr", "ar", "أهلاً وسهلاً");
        dict("oui", "fr", "ar", "نعم");
        dict("non", "fr", "ar", "لا");
        dict("comment allez-vous", "fr", "ar", "كيف حالك");
        dict("au revoir", "fr", "ar", "مع السلامة");
        dict("s'il vous plaît", "fr", "ar", "من فضلك");
        dict("excusez-moi", "fr", "ar", "عذراً");
        dict("je suis", "fr", "ar", "أنا");
        dict("bien", "fr", "ar", "جيد");
        dict("très bien", "fr", "ar", "جيد جداً");
        dict("comment", "fr", "ar", "كيف");
        dict("pourquoi", "fr", "ar", "لماذا");
        dict("quand", "fr", "ar", "متى");
        dict("travail", "fr", "ar", "عمل");
        dict("formation", "fr", "ar", "تدريب");
        dict("emploi", "fr", "ar", "وظيفة");
        dict("recherche", "fr", "ar", "بحث");
        dict("communauté", "fr", "ar", "مجتمع");
        dict("message", "fr", "ar", "رسالة");
        dict("groupe", "fr", "ar", "مجموعة");
        dict("événement", "fr", "ar", "حدث");
        dict("profil", "fr", "ar", "ملف شخصي");

        // ── Français → Anglais ──
        dict("bonjour", "fr", "en", "Hello");
        dict("bonsoir", "fr", "en", "Good evening");
        dict("salut", "fr", "en", "Hi");
        dict("merci", "fr", "en", "Thank you");
        dict("bienvenue", "fr", "en", "Welcome");
        dict("oui", "fr", "en", "Yes");
        dict("non", "fr", "en", "No");
        dict("au revoir", "fr", "en", "Goodbye");
        dict("comment allez-vous", "fr", "en", "How are you");
        dict("bien", "fr", "en", "Good");
        dict("très bien", "fr", "en", "Very good");
        dict("travail", "fr", "en", "Work");
        dict("formation", "fr", "en", "Training");
        dict("emploi", "fr", "en", "Job");
        dict("recherche", "fr", "en", "Search");

        // ── Anglais → Arabe ──
        dict("hello", "en", "ar", "مرحبا");
        dict("hi", "en", "ar", "أهلاً");
        dict("good morning", "en", "ar", "صباح الخير");
        dict("good evening", "en", "ar", "مساء الخير");
        dict("thank you", "en", "ar", "شكراً");
        dict("yes", "en", "ar", "نعم");
        dict("no", "en", "ar", "لا");
        dict("goodbye", "en", "ar", "مع السلامة");
        dict("please", "en", "ar", "من فضلك");
        dict("welcome", "en", "ar", "أهلاً وسهلاً");
        dict("how are you", "en", "ar", "كيف حالك");
        dict("good", "en", "ar", "جيد");
        dict("work", "en", "ar", "عمل");
        dict("training", "en", "ar", "تدريب");
        dict("job", "en", "ar", "وظيفة");
        dict("search", "en", "ar", "بحث");
        dict("community", "en", "ar", "مجتمع");

        // ── Anglais → Français ──
        dict("hello", "en", "fr", "Bonjour");
        dict("hi", "en", "fr", "Salut");
        dict("thank you", "en", "fr", "Merci");
        dict("yes", "en", "fr", "Oui");
        dict("no", "en", "fr", "Non");
        dict("goodbye", "en", "fr", "Au revoir");
        dict("welcome", "en", "fr", "Bienvenue");
        dict("good", "en", "fr", "Bien");

        // ── Arabe → Français ──
        dict("مرحبا", "ar", "fr", "Bonjour");
        dict("شكراً", "ar", "fr", "Merci");
        dict("نعم", "ar", "fr", "Oui");
        dict("لا", "ar", "fr", "Non");
        dict("أهلاً", "ar", "fr", "Salut");
        dict("مع السلامة", "ar", "fr", "Au revoir");

        // ── Arabe → Anglais ──
        dict("مرحبا", "ar", "en", "Hello");
        dict("شكراً", "ar", "en", "Thank you");
        dict("نعم", "ar", "en", "Yes");
        dict("لا", "ar", "en", "No");
    }

    /** Méthode utilitaire pour ajouter une entrée au dictionnaire local */
    private static void dict(String text, String from, String to, String translation) {
        LOCAL_DICT.put(text.toLowerCase().trim() + "|" + from + "|" + to, translation);
    }

    // Langues disponibles pour la traduction
    /** Map des langues supportées : code ISO → nom affiché */
    public static final Map<String, String> SUPPORTED_LANGUAGES = new LinkedHashMap<>();
    static {
        SUPPORTED_LANGUAGES.put("fr", "Français");
        SUPPORTED_LANGUAGES.put("en", "English");
        SUPPORTED_LANGUAGES.put("ar", "العربية");
    }

    /** Constructeur privé (Singleton) */
    private TranslationService() {}

    /**
     * Retourne l'instance unique du TranslationService.
     * Double-Checked Locking pour la sécurité multi-thread.
     */
    public static TranslationService getInstance() {
        if (instance == null) {
            synchronized (TranslationService.class) {
                if (instance == null) {
                    instance = new TranslationService();
                }
            }
        }
        return instance;
    }

    /**
     * Traduit un texte d'une langue source vers une langue cible.
     *
     * Stratégie multi-couche :
     *   1. Vérifier le cache LRU
     *   2. Chercher dans le dictionnaire local intégré
     *   3. Appeler l'API MyMemory avec filtrage qualité
     *   4. Si fr→ar échoue, essayer fr→en→ar (traduction en 2 étapes)
     *   5. En dernier recours, retourner le texte original
     *
     * @param text       le texte à traduire (max 5000 caractères)
     * @param sourceLang le code ISO de la langue source (ex: "fr", "en", "ar")
     * @param targetLang le code ISO de la langue cible (ex: "en", "fr", "ar")
     * @return le texte traduit, ou le texte original en cas d'erreur
     */
    public String translate(String text, String sourceLang, String targetLang) {
        // Vérification des paramètres : si texte vide ou même langue → retourner tel quel
        if (text == null || text.isBlank()) return text;

        // Si la langue source détectée == langue cible, re-détecter plus intelligemment
        // Cela corrige le cas où detectLanguage() se trompe et retourne la même langue
        if (sourceLang.equals(targetLang)) {
            // Essayer de trouver une meilleure langue source
            String cleaned = stripEmojisAndSpecialChars(text).toLowerCase().trim();
            // Vérifier dans le dictionnaire local si le texte existe dans une autre langue
            for (String lang : SUPPORTED_LANGUAGES.keySet()) {
                if (!lang.equals(targetLang)) {
                    String tryKey = cleaned + "|" + lang + "|" + targetLang;
                    if (LOCAL_DICT.containsKey(tryKey)) {
                        sourceLang = lang;
                        logger.info("Auto-corrected source lang from {} to {} for text: {}", targetLang, lang, text);
                        break;
                    }
                }
            }
            // Si toujours la même langue après vérification, tenter via l'API quand même
            if (sourceLang.equals(targetLang)) {
                // Essayer en forçant les langues alternatives courantes
                for (String altSource : new String[]{"fr", "en", "ar"}) {
                    if (!altSource.equals(targetLang)) {
                        String apiResult = callMyMemoryAPI(text, altSource, targetLang);
                        if (apiResult != null && isValidTranslation(apiResult, text, targetLang)) {
                            cache.put(text + "|" + altSource + "|" + targetLang, apiResult);
                            logger.info("Forced translation [{}→{}] : {} → {}", altSource, targetLang,
                                    text.substring(0, Math.min(30, text.length())),
                                    apiResult.substring(0, Math.min(30, apiResult.length())));
                            return apiResult;
                        }
                    }
                }
                return text; // Vraiment la même langue, retourner tel quel
            }
        }

        // ── COUCHE 1 : Vérifier le cache ──
        String cacheKey = text + "|" + sourceLang + "|" + targetLang;
        if (cache.containsKey(cacheKey)) {
            String cached = cache.get(cacheKey);
            // Invalider les entrées cachées pourries (garbage des sessions précédentes)
            if (isValidTranslation(cached, text, targetLang)) {
                logger.debug("Cache hit: {}", cacheKey.substring(0, Math.min(50, cacheKey.length())));
                return cached;
            } else {
                cache.remove(cacheKey); // Purger l'entrée invalide du cache
                logger.debug("Cache entry invalidated (garbage): {}", cacheKey.substring(0, Math.min(50, cacheKey.length())));
            }
        }

        // ── COUCHE 2 : Dictionnaire local intégré (instantané, fiable) ──
        // Essayer d'abord avec le texte original
        String dictKey = text.toLowerCase().trim() + "|" + sourceLang + "|" + targetLang;
        if (LOCAL_DICT.containsKey(dictKey)) {
            String dictResult = LOCAL_DICT.get(dictKey);
            cache.put(cacheKey, dictResult);
            logger.info("Dict translation [{}→{}] : {} → {}", sourceLang, targetLang, text, dictResult);
            return dictResult;
        }
        // Essayer aussi avec le texte nettoyé (sans emojis)
        String cleanedText = stripEmojisAndSpecialChars(text).toLowerCase().trim();
        String cleanedDictKey = cleanedText + "|" + sourceLang + "|" + targetLang;
        if (!cleanedDictKey.equals(dictKey) && LOCAL_DICT.containsKey(cleanedDictKey)) {
            String dictResult = LOCAL_DICT.get(cleanedDictKey);
            cache.put(cacheKey, dictResult);
            logger.info("Dict translation (cleaned) [{}→{}] : {} → {}", sourceLang, targetLang, text, dictResult);
            return dictResult;
        }

        // ── COUCHE 3 : API MyMemory (traduction machine) ──
        String apiResult = callMyMemoryAPI(text, sourceLang, targetLang);
        if (apiResult != null && isValidTranslation(apiResult, text, targetLang)) {
            cache.put(cacheKey, apiResult);
            logger.info("API translation [{}→{}] : {} → {}", sourceLang, targetLang,
                    text.substring(0, Math.min(30, text.length())),
                    apiResult.substring(0, Math.min(30, apiResult.length())));
            return apiResult;
        }

        // ── COUCHE 4 : Traduction en 2 étapes via l'anglais (pour fr↔ar) ──
        // MyMemory est meilleur pour fr→en et en→ar séparément
        if (!"en".equals(sourceLang) && !"en".equals(targetLang)) {
            logger.info("Direct translation failed, trying 2-step via English: {}→en→{}", sourceLang, targetLang);

            // Étape A : source → anglais
            String dictKeyA = text.toLowerCase().trim() + "|" + sourceLang + "|en";
            String englishText = LOCAL_DICT.containsKey(dictKeyA) ? LOCAL_DICT.get(dictKeyA)
                    : callMyMemoryAPI(text, sourceLang, "en");

            if (englishText != null && isValidTranslation(englishText, text, "en")) {
                // Étape B : anglais → cible
                String dictKeyB = englishText.toLowerCase().trim() + "|en|" + targetLang;
                String finalResult = LOCAL_DICT.containsKey(dictKeyB) ? LOCAL_DICT.get(dictKeyB)
                        : callMyMemoryAPI(englishText, "en", targetLang);

                if (finalResult != null && isValidTranslation(finalResult, text, targetLang)) {
                    cache.put(cacheKey, finalResult);
                    logger.info("2-step translation [{}→en→{}] : {} → {} → {}", sourceLang, targetLang,
                            text.substring(0, Math.min(20, text.length())),
                            englishText.substring(0, Math.min(20, englishText.length())),
                            finalResult.substring(0, Math.min(20, finalResult.length())));
                    return finalResult;
                }
            }
        }

        // ── COUCHE 5 : Échec total → retourner le texte original ──
        logger.warn("All translation strategies failed for [{}→{}]: {}", sourceLang, targetLang, text);
        return text;
    }

    /**
     * Appelle l'API MyMemory et retourne la meilleure traduction trouvée.
     * Utilise le paramètre &mt=1 pour forcer la traduction machine.
     * Parcourt le tableau "matches" pour trouver le résultat le plus fiable.
     *
     * @param text       le texte à traduire
     * @param sourceLang la langue source (code ISO)
     * @param targetLang la langue cible (code ISO)
     * @return la meilleure traduction trouvée, ou null si aucune valide
     */
    private String callMyMemoryAPI(String text, String sourceLang, String targetLang) {
        try {
            // Nettoyer le texte avant envoi (supprimer emojis qui perturbent l'API)
            String cleanText = stripEmojisAndSpecialChars(text);
            if (cleanText.isBlank()) {
                logger.warn("Text is empty after cleaning, cannot translate");
                return null;
            }

            // Construire l'URL avec &mt=1 pour forcer la traduction machine
            String encodedText = URLEncoder.encode(cleanText, StandardCharsets.UTF_8);
            String langPair = sourceLang + "|" + targetLang;
            String urlString = API_URL + "?q=" + encodedText + "&langpair=" + langPair + "&mt=1";

            logger.debug("MyMemory API call: [{}→{}] '{}'", sourceLang, targetLang, cleanText);

            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setRequestProperty("User-Agent", "Skilora/1.0");

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                logger.warn("MyMemory API returned HTTP {}", responseCode);
                return null;
            }

            // Lire la réponse JSON
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            // Parser le JSON
            JSONObject jsonResponse = new JSONObject(response.toString());
            JSONObject responseData = jsonResponse.getJSONObject("responseData");
            String primaryTranslation = responseData.getString("translatedText");
            double primaryScore = responseData.optDouble("match", 0.0);

            logger.debug("MyMemory primary: '{}' (match={})", primaryTranslation, primaryScore);

            // ── Chercher la meilleure traduction dans "matches" ──
            // Priorité : entrées MT (machine translation) > mémoire de traduction
            String bestMT = null;       // Meilleure traduction machine
            String bestTM = null;       // Meilleure mémoire de traduction
            double bestMTScore = -1;
            double bestTMScore = -1;

            if (jsonResponse.has("matches")) {
                JSONArray matches = jsonResponse.getJSONArray("matches");
                for (int i = 0; i < matches.length(); i++) {
                    JSONObject m = matches.getJSONObject(i);
                    String trans = m.optString("translation", "");
                    double score = m.optDouble("match", 0.0);
                    String createdBy = m.optString("created-by", "");

                    if (trans.isEmpty() || trans.equalsIgnoreCase(cleanText)) continue;
                    if (trans.toUpperCase().contains("MYMEMORY WARNING")) continue;
                    if (!isValidTranslation(trans, cleanText, targetLang)) continue;

                    // Séparer MT (machine) et TM (mémoire de traduction)
                    if (createdBy.contains("MT")) {
                        if (score > bestMTScore) { bestMTScore = score; bestMT = trans; }
                    } else {
                        if (score > bestTMScore) { bestTMScore = score; bestTM = trans; }
                    }
                }
            }

            // ── Choisir la meilleure traduction ──
            // Préférer MT si disponible (plus fiable que les mémoires de traduction crowd-sourced)
            if (bestMT != null) {
                logger.debug("Using MT result: '{}'", bestMT);
                return bestMT;
            }

            // Sinon, utiliser la mémoire de traduction (seuil bas pour accepter plus)
            if (bestTM != null && bestTMScore >= 0.3) {
                logger.debug("Using TM result (score={}): '{}'", bestTMScore, bestTM);
                return bestTM;
            }

            // En dernier recours, la traduction primaire si valide
            // C'est le résultat principal de l'API — l'accepter plus facilement
            if (primaryTranslation != null && !primaryTranslation.isBlank()
                    && !primaryTranslation.toUpperCase().contains("MYMEMORY WARNING")
                    && !primaryTranslation.equalsIgnoreCase(cleanText)) {
                logger.debug("Using primary translation: '{}'", primaryTranslation);
                return primaryTranslation;
            }

        } catch (Exception e) {
            logger.error("MyMemory API error: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Supprime les emojis et caractères spéciaux non-alphabétiques d'un texte.
     * Conserve les lettres (y compris accentuées), chiffres et espaces.
     * Utilisé pour normaliser le texte avant détection de langue ou recherche dictionnaire.
     *
     * @param text le texte à nettoyer
     * @return le texte sans emojis ni caractères spéciaux
     */
    private String stripEmojisAndSpecialChars(String text) {
        if (text == null) return "";
        // Garder les lettres (toutes langues), chiffres, espaces, apostrophes et tirets
        return text.replaceAll("[^\\p{L}\\p{N}\\s'-]", "").trim();
    }

    /**
     * Détecte automatiquement la langue d'un texte.
     * Utilise des heuristiques robustes basées sur :
     *   - Caractères arabes → "ar"
     *   - Lettres accentuées françaises (é, è, ê, ç, à, ù, etc.) → "fr"
     *   - Contractions françaises (j', l', d', c'est, qu', n', s') → "fr"
     *   - Mots courants français (vocabulaire étendu) → "fr"
     *   - Par défaut → "fr" (la majorité des utilisateurs Skilora Tunisie parlent français)
     *
     * @param text le texte à analyser
     * @return le code ISO de la langue détectée ("fr", "en", ou "ar")
     */
    public String detectLanguage(String text) {
        if (text == null || text.isBlank()) return "fr";

        // ── Étape 0 : Nettoyer le texte (supprimer emojis et caractères spéciaux) ──
        String cleaned = stripEmojisAndSpecialChars(text);
        if (cleaned.isBlank()) return "fr";

        // ── Étape 1 : Détection arabe (caractères Unicode \u0600-\u06FF) ──
        long arabicChars = cleaned.chars().filter(c -> c >= 0x0600 && c <= 0x06FF).count();
        long totalLetters = cleaned.chars().filter(Character::isLetter).count();
        if (totalLetters > 0 && arabicChars > totalLetters * 0.3) {
            return "ar";
        }

        String lower = cleaned.toLowerCase().trim();

        // ── Étape 2 : Détection français — lettres accentuées typiques ──
        // Les accents français (é, è, ê, ë, à, â, ù, û, ç, ô, î, ï) sont rares en anglais
        long frenchAccents = lower.chars().filter(c ->
                c == 'é' || c == 'è' || c == 'ê' || c == 'ë' ||
                c == 'à' || c == 'â' || c == 'ù' || c == 'û' ||
                c == 'ç' || c == 'ô' || c == 'î' || c == 'ï' || c == 'ü'
        ).count();
        if (frenchAccents > 0) return "fr";

        // ── Étape 3 : Détection français — contractions typiques ──
        // j'adore, l'homme, d'accord, c'est, qu'il, n'est, s'il
        if (lower.matches(".*\\b[jldcnqs]'\\w+.*")) return "fr";

        // ── Étape 4 : Détection français — mot isolé (vocabulaire étendu) ──
        Set<String> frenchSingleWords = Set.of(
                "bonjour", "salut", "merci", "bienvenue", "oui", "non", "bien",
                "bonsoir", "comment", "pourquoi", "aussi", "alors", "toujours",
                "jamais", "peut", "tous", "très", "même", "encore", "ici",
                "matin", "soir", "jour", "nuit", "homme", "femme", "enfant",
                "maison", "travail", "école", "voiture", "ville", "pays",
                "communauté", "groupe", "profil", "message", "emploi",
                "recherche", "formation", "événement", "nouveau", "nouvelle",
                "petit", "grand", "monde", "temps", "année", "chose",
                "gens", "avoir", "être", "faire", "aller", "venir",
                "prendre", "mettre", "donner", "parler", "partir", "aimer",
                "voir", "savoir", "pouvoir", "vouloir", "devoir", "falloir",
                "aujourd", "demain", "hier", "maintenant", "bientôt",
                "beaucoup", "quelque", "chaque", "autre", "plusieurs"
        );
        // Nettoyer et vérifier si c'est un seul mot français
        String singleWord = lower.replaceAll("\\s+", "");
        if (!lower.contains(" ") && frenchSingleWords.contains(singleWord)) {
            return "fr";
        }

        // ── Étape 5 : Détection français — mots-outils dans des phrases ──
        String[] frenchWords = {"le", "la", "les", "de", "du", "des", "un", "une", "est",
                "et", "en", "que", "qui", "pour", "dans", "sur", "avec", "pas", "ce",
                "je", "tu", "il", "elle", "nous", "vous", "ils", "elles", "on",
                "ne", "se", "au", "aux", "son", "sa", "ses", "mon", "ma", "mes",
                "ton", "ta", "tes", "notre", "votre", "leur", "leurs",
                "mais", "ou", "donc", "car", "ni", "très", "plus", "moins",
                "cette", "ces", "cet", "comme", "tout", "tous", "toute"};
        int frenchCount = 0;
        // Découper en mots pour une détection plus fiable
        String[] words = lower.split("[\\s']+");
        Set<String> frenchWordSet = Set.of(frenchWords);
        for (String w : words) {
            if (frenchWordSet.contains(w)) {
                frenchCount++;
            }
        }
        // 1 seul mot-outil français suffit pour un texte court
        if (frenchCount >= 1 && words.length <= 5) return "fr";
        if (frenchCount >= 2) return "fr";

        // ── Étape 6 : Détection anglais — mots-outils anglais typiques ──
        String[] englishWords = {"the", "is", "are", "was", "were", "have", "has", "had",
                "do", "does", "did", "will", "would", "could", "should", "can",
                "this", "that", "these", "those", "with", "from", "into",
                "about", "than", "been", "being", "which", "what", "where", "when"};
        int englishCount = 0;
        Set<String> englishWordSet = Set.of(englishWords);
        for (String w : words) {
            if (englishWordSet.contains(w)) {
                englishCount++;
            }
        }
        if (englishCount >= 2) return "en";
        if (englishCount >= 1 && frenchCount == 0) return "en";

        // ── Par défaut : français (contexte Skilora Tunisie) ──
        return "fr";
    }

    /**
     * Vérifie la qualité d'une traduction retournée par MyMemory.
     * Filtre les résultats "poubelle" (garbage translations) :
     *   - Traduction identique au texte source
     *   - Contient des caractères suspects (c/, @, #, http)
     *   - Traduction anormalement longue par rapport au texte source
     *   - Traduction vers l'arabe ne contient aucun caractère arabe
     *
     * @param translation le texte traduit à valider
     * @param original    le texte original
     * @param targetLang  la langue cible
     * @return true si la traduction semble valide
     */
    private boolean isValidTranslation(String translation, String original, String targetLang) {
        if (translation == null || translation.isBlank()) return false;

        // Nettoyer les deux textes pour une comparaison juste (sans emojis)
        String cleanTranslation = stripEmojisAndSpecialChars(translation).trim();
        String cleanOriginal = stripEmojisAndSpecialChars(original).trim();

        // Ignorer si identique au texte source (comparaison nettoyée)
        if (cleanTranslation.equalsIgnoreCase(cleanOriginal)) return false;

        // Filtrer les traductions contenant des patterns suspects (fragments techniques, URLs)
        if (translation.contains("http") || translation.contains("@")
                || translation.contains("<") || translation.contains(">")) return false;

        // Si traduction abnormalement longue (5x+ le texte source) → probablement garbage
        if (cleanTranslation.length() > cleanOriginal.length() * 5 + 50) return false;

        // Pour la cible arabe : vérifier qu'il y a bien des caractères arabes
        if ("ar".equals(targetLang)) {
            long arabicChars = translation.chars().filter(c -> c >= 0x0600 && c <= 0x06FF).count();
            if (arabicChars == 0) return false;
        }
        return true;
    }

    /**
     * Vide le cache de traduction.
     * Utile si l'utilisateur veut forcer un re-traduction.
     */
    public void clearCache() {
        cache.clear();
        logger.info("Translation cache cleared");
    }
}
