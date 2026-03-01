package com.skilora.service.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.skilora.model.entity.formation.Training;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI Service for chatbot functionality using Groq API (free)
 */
public class AIService {
    private static final Logger logger = LoggerFactory.getLogger(AIService.class);
    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.1-8b-instant"; // Updated: llama3-8b-8192 was decommissioned
    private static volatile AIService instance;
    
    static {
        // Log configuration at class load time
        logger.info("=== AIService Configuration ===");
        logger.info("Groq API URL: {}", GROQ_API_URL);
        logger.info("Model: {}", MODEL);
        logger.info("NOTE: Using Groq API (free) - direct API call from JavaFX");
        logger.info("================================");
    }
    
    private final String apiKey;
    private final Gson gson = new Gson();
    
    // Session memory: Map of session ID to conversation history
    private final Map<String, List<JsonObject>> sessionMemory = new ConcurrentHashMap<>();
    
    // Language detection: Map of session ID to detected language
    private final Map<String, String> sessionLanguage = new ConcurrentHashMap<>();
    
    private AIService() {
        logger.info("========================================");
        logger.info("AIService CONSTRUCTOR - Loading Groq API Key");
        logger.info("========================================");
        
        // Get API key from environment variable or system property (Groq)
        String envKey = System.getenv("GROQ_API_KEY");
        logger.info("DEBUG: Checking System.getenv(\"GROQ_API_KEY\")");
        logger.info("DEBUG: Result is null: {}", envKey == null);
        if (envKey != null) {
            logger.info("DEBUG: Result is blank: {}", envKey.isBlank());
            logger.info("DEBUG: Result length: {}", envKey.length());
        }
        logger.info("Checking for GROQ_API_KEY in environment: {}", envKey != null && !envKey.isBlank() ? "FOUND (length: " + envKey.length() + ")" : "NOT FOUND");
        
        if (envKey == null || envKey.isBlank()) {
            logger.info("DEBUG: Environment variable not found, checking system property...");
            envKey = System.getProperty("groq.api.key");
            logger.info("DEBUG: System.getProperty(\"groq.api.key\") result is null: {}", envKey == null);
            if (envKey != null) {
                logger.info("DEBUG: System property result is blank: {}", envKey.isBlank());
                logger.info("DEBUG: System property result length: {}", envKey.length());
            }
            logger.info("Checking for groq.api.key system property: {}", envKey != null && !envKey.isBlank() ? "FOUND (length: " + envKey.length() + ")" : "NOT FOUND");
        }
        
        // Try loading from application.properties as fallback
        if ((envKey == null || envKey.isBlank())) {
            logger.info("DEBUG: Trying to load from application.properties...");
            try {
                java.util.Properties props = new java.util.Properties();
                String propertiesPath = "config/application.properties";
                logger.info("DEBUG: Loading from resource path: {}", propertiesPath);
                java.io.InputStream is = getClass().getClassLoader().getResourceAsStream(propertiesPath);
                if (is != null) {
                    logger.info("DEBUG: Properties file found, loading...");
                    props.load(is);
                    envKey = props.getProperty("groq.api.key");
                    logger.info("DEBUG: Property 'groq.api.key' value is null: {}", envKey == null);
                    if (envKey != null) {
                        logger.info("DEBUG: Property 'groq.api.key' value is blank: {}", envKey.isBlank());
                        logger.info("DEBUG: Property 'groq.api.key' value length: {}", envKey.length());
                        logger.info("DEBUG: Found in application.properties: {}", !envKey.isBlank());
                        if (!envKey.isBlank()) {
                            logger.info("DEBUG: API key from properties length: {}", envKey.length());
                            logger.info("DEBUG: API key from properties starts with: {}", envKey.length() > 7 ? envKey.substring(0, 7) + "..." : "***");
                        }
                    } else {
                        logger.warn("DEBUG: Property 'groq.api.key' not found in properties file");
                        // List all properties for debugging
                        logger.info("DEBUG: All properties in file:");
                        props.stringPropertyNames().forEach(key -> {
                            if (key.contains("api") || key.contains("key") || key.contains("groq")) {
                                logger.info("DEBUG:   {} = {}", key, props.getProperty(key).length() > 20 ? props.getProperty(key).substring(0, 20) + "..." : props.getProperty(key));
                            }
                        });
                    }
                    is.close();
                } else {
                    logger.error("DEBUG: application.properties file NOT FOUND at path: {}", propertiesPath);
                    logger.error("DEBUG: Make sure the file exists at: src/main/resources/config/application.properties");
                }
            } catch (Exception e) {
                logger.error("DEBUG: Exception loading from application.properties: {}", e.getMessage(), e);
            }
        }
        
        this.apiKey = envKey != null ? envKey : "";
        
        // STEP 1: Print API key (first 10 characters only for security)
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║                    STEP 1: API KEY CHECK                      ║");
        System.out.println("╠═══════════════════════════════════════════════════════════════╣");
        if (!this.apiKey.isBlank()) {
            System.out.println("║ GROQ KEY: " + String.format("%-50s", this.apiKey.substring(0, Math.min(10, this.apiKey.length())) + "...") + "║");
        } else {
            System.out.println("║ GROQ KEY: NOT FOUND (blank or null)                        ║");
        }
        System.out.println("╚═══════════════════════════════════════════════════════════════╝");
        logger.info("STEP 1: GROQ KEY (first 10 chars): {}", !this.apiKey.isBlank() ? this.apiKey.substring(0, Math.min(10, this.apiKey.length())) + "..." : "NOT FOUND");
        
        logger.info("========================================");
        if (this.apiKey.isBlank()) {
            System.err.println("╔═══════════════════════════════════════════════════════════════╗");
            System.err.println("║              ⚠️  GROQ API KEY IS MISSING! ⚠️                  ║");
            System.err.println("╠═══════════════════════════════════════════════════════════════╣");
            System.err.println("║ Tried: System.getenv(\"GROQ_API_KEY\")                        ║");
            System.err.println("║ Tried: System.getProperty(\"groq.api.key\")                   ║");
            System.err.println("║ Tried: application.properties (groq.api.key)                  ║");
            System.err.println("║                                                                 ║");
            System.err.println("║ SOLUTION: Add your Groq API key to:                          ║");
            System.err.println("║   src/main/resources/config/application.properties           ║");
            System.err.println("║   Format: groq.api.key=your_groq_api_key_here                 ║");
            System.err.println("║   Get free key at: https://console.groq.com/keys              ║");
            System.err.println("╚═══════════════════════════════════════════════════════════════╝");
            logger.warn("GROQ API KEY IS MISSING!");
            logger.warn("Tried: System.getenv(\"GROQ_API_KEY\")");
            logger.warn("Tried: System.getProperty(\"groq.api.key\")");
            logger.warn("Tried: application.properties (groq.api.key)");
            logger.warn("RESULT: API KEY IS BLANK - CHATBOT WILL NOT WORK");
            logger.warn("SOLUTION: Add your Groq API key to src/main/resources/config/application.properties");
            logger.warn("  Format: groq.api.key=your_groq_api_key_here");
            logger.warn("  Get free key at: https://console.groq.com/keys");
        } else {
            System.out.println("╔═══════════════════════════════════════════════════════════════╗");
            System.out.println("║          ✅ GROQ API KEY LOADED SUCCESSFULLY ✅                ║");
            System.out.println("╠═══════════════════════════════════════════════════════════════╣");
            System.out.println("║ Source: " + String.format("%-52s", 
                       System.getenv("GROQ_API_KEY") != null && !System.getenv("GROQ_API_KEY").isBlank() ? "Environment variable" :
                       System.getProperty("groq.api.key") != null && !System.getProperty("groq.api.key").isBlank() ? "System property" :
                       "application.properties") + "║");
            System.out.println("║ Length: " + String.format("%-53s", this.apiKey.length() + " characters") + "║");
            System.out.println("║ Starts with: " + String.format("%-48s", this.apiKey.length() > 7 ? this.apiKey.substring(0, 7) + "..." : "***") + "║");
            System.out.println("║ API URL: " + String.format("%-51s", GROQ_API_URL) + "║");
            System.out.println("║ Model: " + String.format("%-54s", MODEL) + "║");
            System.out.println("╚═══════════════════════════════════════════════════════════════╝");
            logger.info("GROQ API KEY LOADED SUCCESSFULLY");
            logger.info("Source: {}", 
                       System.getenv("GROQ_API_KEY") != null && !System.getenv("GROQ_API_KEY").isBlank() ? "Environment variable" :
                       System.getProperty("groq.api.key") != null && !System.getProperty("groq.api.key").isBlank() ? "System property" :
                       "application.properties");
            logger.info("Length: {} characters", this.apiKey.length());
            logger.info("Starts with: {}", this.apiKey.length() > 7 ? this.apiKey.substring(0, 7) + "..." : "***");
            logger.info("API URL: {}", GROQ_API_URL);
            logger.info("Model: {}", MODEL);
        }
        logger.info("========================================");
    }
    
    public static synchronized AIService getInstance() {
        if (instance == null) {
            instance = new AIService();
        }
        return instance;
    }
    
    /**
     * Check if AI service is configured
     */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
    
    /**
     * Detect language from user message
     */
    private String detectLanguage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "fr"; // Default to French
        }
        
        String lower = message.toLowerCase();
        
        // Arabic detection (contains Arabic characters)
        if (message.matches(".*[\\u0600-\\u06FF].*")) {
            return "ar";
        }
        
        // English detection (common English words)
        String[] englishWords = {"the", "is", "are", "what", "how", "can", "you", "your", "this", "that", "with", "for", "from"};
        int englishCount = 0;
        for (String word : englishWords) {
            if (lower.contains(" " + word + " ") || lower.startsWith(word + " ") || lower.endsWith(" " + word)) {
                englishCount++;
            }
        }
        
        // French detection (common French words)
        String[] frenchWords = {"le", "la", "les", "de", "des", "du", "est", "sont", "vous", "votre", "comment", "quelle", "quelles", "formation", "formations"};
        int frenchCount = 0;
        for (String word : frenchWords) {
            if (lower.contains(" " + word + " ") || lower.startsWith(word + " ") || lower.endsWith(" " + word)) {
                frenchCount++;
            }
        }
        
        // Determine language based on word matches
        if (englishCount > frenchCount && englishCount > 0) {
            return "en";
        } else if (frenchCount > 0) {
            return "fr";
        }
        
        // Default to French
        return "fr";
    }
    
    /**
     * Build system prompt with formations data and enhanced capabilities
     */
    private String buildSystemPrompt(List<Training> formations, String language) {
        StringBuilder prompt = new StringBuilder();
        
        // Log formations count for debugging
        logger.info("Building system prompt with {} formations", formations != null ? formations.size() : 0);
        if (formations == null || formations.isEmpty()) {
            logger.warn("WARNING: Formations list is empty or null!");
        }
        
        // Language-specific greeting - More detailed and expert-like
        if ("ar".equals(language)) {
            prompt.append("أنت مساعد خبير لمنصة Skilora Tunisia للتعلم الإلكتروني. لديك إمكانية الوصول إلى القائمة الكاملة للتكوينات المتاحة. استخدم لغة مهنية واحترافية في جميع ردودك. تجنب تماماً استخدام الرموز أو الإيموجي أو الأحرف الخاصة أو الاختصارات غير الرسمية. استخدم فقط نصاً مهنياً ورسمياً مع جمل كاملة وواضحة. ");
        } else if ("en".equals(language)) {
            prompt.append("You are an expert assistant for Skilora Tunisia e-learning platform. You have access to the complete list of available formations. Use professional and formal language in all your responses. Absolutely avoid using symbols, emojis, special characters, or informal abbreviations. Use only professional and formal text with complete and clear sentences. ");
        } else {
            prompt.append("Vous êtes un assistant expert pour la plateforme d'apprentissage en ligne Skilora Tunisia. Vous avez accès à la liste complète des formations disponibles. Utilisez un langage professionnel et formel dans toutes vos réponses. Évitez absolument d'utiliser des symboles, emojis, caractères spéciaux ou abréviations informelles. Utilisez uniquement du texte professionnel et formel avec des phrases complètes et claires. ");
        }
        
        // Convert formations to JSON format for context - with detailed logging
        JsonArray formationsJson = new JsonArray();
        if (formations != null && !formations.isEmpty()) {
            logger.info("Processing {} formations for system prompt", formations.size());
            for (Training training : formations) {
                JsonObject form = new JsonObject();
                form.addProperty("id", training.getId());
                form.addProperty("title", training.getTitle() != null ? training.getTitle() : "");
                form.addProperty("description", training.getDescription() != null ? training.getDescription() : "");
                form.addProperty("level", training.getLevel() != null ? training.getLevel().name() : "");
                form.addProperty("levelDisplay", training.getLevel() != null ? training.getLevel().getDisplayName() : "");
                form.addProperty("duration", training.getDuration());
                form.addProperty("category", training.getCategory() != null ? training.getCategory().name() : "");
                form.addProperty("categoryDisplay", training.getCategory() != null ? training.getCategory().getDisplayName() : "");
                form.addProperty("price", training.getCost() != null ? training.getCost() : 0.0);
                form.addProperty("isFree", training.isFree());
                form.addProperty("lessonCount", training.getLessonCount());
                formationsJson.add(form);
                logger.debug("Added formation to prompt: {} (ID: {})", training.getTitle(), training.getId());
            }
            logger.info("Total formations added to prompt: {}", formationsJson.size());
        } else {
            logger.error("ERROR: Formations list is empty! The chatbot will not have access to formation data.");
        }
        
        prompt.append("\n\n");
        if ("ar".equals(language)) {
            prompt.append("قائمة التكوينات المتاحة الكاملة (استخدم هذه المعلومات فقط):\n");
        } else if ("en".equals(language)) {
            prompt.append("Complete list of available formations (USE ONLY THIS DATA - DO NOT INVENT ANYTHING):\n");
        } else {
            prompt.append("Liste complète des formations disponibles (UTILISEZ UNIQUEMENT CES DONNÉES - N'INVENTEZ RIEN):\n");
        }
        prompt.append(formationsJson.toString());
        prompt.append("\n\n");
        
        // Log the formations JSON size for debugging
        String formationsJsonStr = formationsJson.toString();
        logger.info("Formations JSON length: {} characters", formationsJsonStr.length());
        logger.debug("Formations JSON (first 500 chars): {}", formationsJsonStr.length() > 500 ? formationsJsonStr.substring(0, 500) + "..." : formationsJsonStr);
        
        if ("ar".equals(language)) {
            prompt.append("مهم جداً: هذه هي جميع التكوينات المتاحة. لا تذكر أي تكوين غير موجود في هذه القائمة.\n");
        } else if ("en".equals(language)) {
            prompt.append("VERY IMPORTANT: These are ALL available formations. Do NOT mention any formation that is NOT in this list.\n");
        } else {
            prompt.append("TRÈS IMPORTANT: Ce sont TOUTES les formations disponibles. Ne mentionnez AUCUNE formation qui n'est PAS dans cette liste.\n");
        }
        prompt.append("\n");
        
        // Enhanced instructions - STRICT: Only answer formation questions with provided data
        if ("ar".equals(language)) {
            prompt.append("قواعد صارمة جداً للإجابات الكاملة والمهنية:\n");
            prompt.append("1. أنت مساعد تكوينات خبير ومحترف. يجب أن ترد فقط على الأسئلة المتعلقة بالتكوينات المذكورة أعلاه أو الشهادات المتعلقة بهذه التكوينات.\n");
            prompt.append("2. استخدم فقط المعلومات الموجودة في قائمة التكوينات أعلاه. لا تخترع أي معلومات.\n");
            prompt.append("3. إذا سأل المستخدم عن تكوين غير موجود في القائمة، قل له بأدب أن هذا التكوين غير متوفر واقترح تكوينات مشابهة من القائمة مع تفاصيلها.\n");
            prompt.append("4. إذا سأل المستخدم عن شهادة، اشرح بشكل مهني وواضح أن كل تكوين مكتمل بنجاح يعطي الحق في الحصول على شهادة رسمية من Skilora Tunisia. الشهادة تشهد على إتمام التكوين بنجاح ويمكن استخدامها لإبراز المهارات المكتسبة. اذكر فقط أسماء/عناوين جميع التكوينات التي تمنح شهادات (isFree: false أو التي لها price > 0). اذكرها جميعاً، وليس فقط بعضها، لكن اعرض فقط الأسماء. استخدم لغة مهنية وتجنب الرموز أو الإيموجي في ردودك.\n");
            prompt.append("5. عندما يصف المستخدم هدفه أو مستواه، قم بتوصية جميع التكوينات المناسبة بناءً على المستوى (BEGINNER, INTERMEDIATE, ADVANCED) من القائمة. اذكر فقط أسماء/عناوين التكوينات، وليس التفاصيل. اذكر جميع التكوينات المطابقة، وليس فقط واحداً أو اثنين.\n");
            prompt.append("6. إذا طلب المستخدم رؤية جميع التكوينات، اذكر فقط أسماء/عناوين جميع التكوينات من القائمة. نظمها حسب الفئة أو المستوى لتسهيل القراءة، لكن اعرض فقط الأسماء، وليس التفاصيل (لا وصف، مستوى، مدة، سعر، إلخ).\n");
            prompt.append("7. إذا طلب المستخدم مقارنة تكوينين، استخدم فقط المعلومات من القائمة وقم بمقارنة مهنية ومفصلة: المستوى، المدة، السعر، الفئة، عدد الدروس، الوصف. اشرح مزايا كل تكوين.\n");
            prompt.append("8. مهم جداً: عند سرد التكوينات، اعرض دائماً جميع التكوينات ذات الصلة، لكن اعرض فقط أسماء/عناوين التكوينات. إذا كان هناك 10 تكوينات تطابق المعايير، اذكر الـ 10 أسماء، وليس التفاصيل. لا تقم أبداً بقائمة جزئية.\n");
            prompt.append("9. مهم جداً: أعط دائماً إجابات كاملة ومهنية. لا تقطع أبداً إجابتك بـ \"...\" أو \"إلخ\". أنهي دائماً إجابتك بالكامل. إذا كنت بحاجة إلى سرد التكوينات، اذكرها جميعاً حتى النهاية. إذا كنت بحاجة إلى شرح شيء ما، اشرحه بالكامل.\n");
            prompt.append("10. تنسيق الإجابة المهنية: استخدم جمل كاملة، منظمة جيداً، مع فقرات واضحة. لقوائم التكوينات، استخدم تنسيقاً واضحاً مع شرطات أو أرقام، لكن اعرض فقط أسماء التكوينات، وليس التفاصيل.\n");
            prompt.append("11. إذا سأل المستخدم عن أي شيء غير متعلق بالتكوينات أو الشهادات، رفض بأدب ومهنية وقل: \"أنا مساعد تكوينات فقط. كيف يمكنني مساعدتك في اختيار التكوين المناسب؟\"\n");
            prompt.append("12. تذكر محتوى المحادثة خلال الجلسة الحالية لإعطاء إجابات متماسكة ومخصصة.\n");
            prompt.append("13. أجب دائماً بنفس اللغة التي يستخدمها المستخدم (الفرنسية، العربية، أو الإنجليزية).\n");
            prompt.append("14. لا تخترع تكوينات أو معلومات غير موجودة في القائمة المقدمة.\n");
            prompt.append("15. تذكير نهائي: يجب أن تكون إجاباتك كاملة ومهنية ولا تُقطع أبداً. عند سرد التكوينات، اعرض فقط أسماء/عناوين التكوينات، وليس التفاصيل (لا وصف، مستوى، مدة، سعر، إلخ). اذكر جميع التكوينات ذات الصلة. أنهي دائماً إجابتك بالكامل. مهم جداً: لا تستخدم أبداً رموزاً أو إيموجي أو أحرفاً خاصة أو اختصارات غير رسمية. استخدم فقط نصاً مهنياً ورسمياً.\n");
        } else if ("en".equals(language)) {
            prompt.append("VERY STRICT RULES FOR COMPLETE AND PROFESSIONAL RESPONSES:\n");
            prompt.append("1. You are an EXPERT AND PROFESSIONAL FORMATIONS ASSISTANT. You MUST ONLY answer questions about the formations listed above or certificates related to these formations.\n");
            prompt.append("2. Use ONLY the information present in the formations list above. Do NOT invent any information.\n");
            prompt.append("3. If the user asks about a formation that is NOT in the list, politely tell them it's not available and suggest similar formations from the list with their details.\n");
            prompt.append("4. If the user asks about certificates, explain professionally and clearly that each successfully completed formation entitles the student to an official Skilora Tunisia certificate. The certificate attests to the successful completion of the training and can be used to showcase acquired skills. List ONLY the NAMES/TITLES of ALL formations that offer certificates (isFree: false or those with price > 0). List ALL of them, not just a few, but show ONLY the names. Use professional language and avoid symbols or emojis in your responses.\n");
            prompt.append("5. When the user describes their goal or level, recommend ALL appropriate formations based on level (BEGINNER, INTERMEDIATE, ADVANCED) from the list. List ONLY the NAMES/TITLES of formations, not the details. List ALL matching formations, not just one or two.\n");
            prompt.append("6. If the user asks to see all formations, list ONLY the NAMES/TITLES of ALL formations from the list. Organize them by category or level for easier reading, but show ONLY the names, not the details (no description, level, duration, price, etc.).\n");
            prompt.append("7. If the user asks to compare two formations, use ONLY information from the list and make a professional and detailed comparison: level, duration, price, category, lesson count, description. Explain the advantages of each formation.\n");
            prompt.append("8. CRITICAL: When listing formations, always show ALL relevant formations, but display ONLY the NAMES/TITLES of formations. If there are 10 formations matching the criteria, list all 10 NAMES, not the details. NEVER make a partial list.\n");
            prompt.append("9. CRITICAL: Always give COMPLETE, PROFESSIONAL, and DETAILED responses. NEVER cut your response with \"...\" or \"etc.\". Always finish your response completely. If you need to list formations, list them ALL to the end. If you need to explain something, explain it completely.\n");
            prompt.append("10. Professional response format: Use complete sentences, well-structured, with clear paragraphs. For formation lists, use a clear format with dashes or numbers, and include ALL details for each formation. ABSOLUTELY AVOID symbols, emojis, special characters, or informal abbreviations. Use only professional and formal text.\n");
            prompt.append("11. If the user asks about anything NOT related to formations or certificates, politely and professionally decline by saying: \"I am a formations assistant only. How can I help you choose the right formation?\"\n");
            prompt.append("12. Remember conversation context within the current session to give coherent and personalized responses.\n");
            prompt.append("13. Always respond in the same language the user is using (French, Arabic, or English).\n");
            prompt.append("14. Never invent formations or information that does not exist in the provided list.\n");
            prompt.append("15. FINAL REMINDER: Your responses must be COMPLETE, PROFESSIONAL, and NEVER cut off. When listing formations, display ONLY the NAMES/TITLES of formations, not the details (description, level, duration, price, etc.). List ALL relevant formations. Always finish your response completely. IMPORTANT: NEVER use symbols, emojis, special characters, or informal abbreviations. Use only professional and formal text.\n");
        } else {
            prompt.append("RÈGLES TRÈS STRICTES POUR RÉPONSES COMPLÈTES ET PROFESSIONNELLES:\n");
            prompt.append("1. Vous êtes un ASSISTANT FORMATIONS EXPERT ET PROFESSIONNEL. Vous DEVEZ répondre UNIQUEMENT aux questions concernant les formations listées ci-dessus ou les certificats liés à ces formations.\n");
            prompt.append("2. Utilisez UNIQUEMENT les informations présentes dans la liste des formations ci-dessus. N'inventez AUCUNE information.\n");
            prompt.append("3. Si l'utilisateur demande une formation qui N'EST PAS dans la liste, dites-lui poliment qu'elle n'est pas disponible et suggérez des formations similaires de la liste avec leurs détails.\n");
            prompt.append("4. Si l'utilisateur demande des certificats, expliquez de manière professionnelle et claire que chaque formation complétée avec succès donne droit à un certificat officiel de Skilora Tunisia. Le certificat atteste de la réussite de la formation et peut être utilisé pour valoriser les compétences acquises. Listez UNIQUEMENT les NOMS/TITRES de TOUTES les formations qui offrent des certificats (isFree: false ou celles avec price > 0). Listez TOUTES, pas seulement quelques-unes, mais affichez SEULEMENT les noms. Utilisez un langage professionnel et évitez les symboles ou emojis dans vos réponses.\n");
            prompt.append("5. Lorsque l'utilisateur décrit son objectif ou son niveau, recommandez TOUTES les formations appropriées basées sur le niveau (BEGINNER, INTERMEDIATE, ADVANCED) de la liste. Listez UNIQUEMENT les NOMS/TITRES des formations, pas les détails. Listez TOUTES les formations correspondantes, pas seulement une ou deux.\n");
            prompt.append("6. Si l'utilisateur demande de voir toutes les formations, listez UNIQUEMENT les NOMS/TITRES de TOUTES les formations de la liste. Organisez-les par catégorie ou niveau pour faciliter la lecture, mais affichez SEULEMENT les noms, pas les détails (pas de description, niveau, durée, prix, etc.).\n");
            prompt.append("7. Si l'utilisateur demande de comparer deux formations, utilisez UNIQUEMENT les informations de la liste et faites une comparaison professionnelle et détaillée : niveau, durée, prix, catégorie, nombre de leçons, description. Expliquez les avantages de chaque formation.\n");
            prompt.append("8. CRITIQUE : Lorsque vous listez des formations, montrez TOUJOURS TOUTES les formations pertinentes, mais affichez UNIQUEMENT les NOMS/TITRES des formations. S'il y a 10 formations correspondant aux critères, listez les 10 NOMS, pas les détails. Ne faites JAMAIS de liste partielle.\n");
            prompt.append("9. CRITIQUE : Donnez TOUJOURS des réponses COMPLÈTES, PROFESSIONNELLES et DÉTAILLÉES. Ne coupez JAMAIS votre réponse avec \"...\" ou \"etc.\". Toujours terminer votre réponse complètement. Si vous devez lister des formations, listez-les TOUTES jusqu'au bout. Si vous devez expliquer quelque chose, expliquez-le complètement.\n");
            prompt.append("10. Format de réponse professionnel : Utilisez des phrases complètes, bien structurées, avec des paragraphes clairs. Pour les listes de formations, utilisez un format clair avec des tirets ou des numéros, et incluez TOUS les détails pour chaque formation. ÉVITEZ ABSOLUMENT les symboles, emojis, caractères spéciaux ou abréviations informelles. Utilisez uniquement du texte professionnel et formel.\n");
            prompt.append("11. Si l'utilisateur pose une question sur quelque chose qui N'EST PAS lié aux formations ou certificats, déclinez poliment et professionnellement en disant: \"Je suis un assistant formations uniquement. Comment puis-je vous aider à choisir la bonne formation ?\"\n");
            prompt.append("12. Mémorisez le contexte de la conversation pendant la session actuelle pour donner des réponses cohérentes et personnalisées.\n");
            prompt.append("13. Répondez toujours dans la même langue que l'utilisateur utilise (français, arabe, ou anglais).\n");
            prompt.append("14. N'inventez jamais de formations ou d'informations qui n'existent pas dans la liste fournie.\n");
            prompt.append("15. RAPPEL FINAL : Vos réponses doivent être COMPLÈTES, PROFESSIONNELLES, et ne JAMAIS être coupées. Lorsque vous listez des formations, affichez UNIQUEMENT les NOMS/TITRES des formations, pas les détails (description, niveau, durée, prix, etc.). Listez TOUTES les formations pertinentes. Terminez toujours votre réponse complètement.\n");
        }
        
        return prompt.toString();
    }
    
    /**
     * Validate user input for security (prevent prompt injection, offensive content, etc.)
     * Returns null if safe, or a polite French error message if unsafe
     */
    private String validateInput(String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return "Veuillez saisir une question concernant nos formations.";
        }
        
        String lower = userMessage.toLowerCase();
        
        // Check for prompt injection attempts (expanded list)
        String[] injectionPatterns = {
            "ignore previous", "forget all", "system:", "assistant:", "you are now",
            "act as", "pretend to be", "roleplay", "disregard", "override",
            "ignore instructions", "forget instructions", "new instructions",
            "you are", "from now on", "your new role", "your new task",
            "disregard previous", "forget everything", "new system prompt"
        };
        
        for (String pattern : injectionPatterns) {
            if (lower.contains(pattern)) {
                logger.warn("Potential prompt injection detected: {}", userMessage);
                return "Je suis désolé, mais je ne peux répondre qu'aux questions concernant les formations Skilora Tunisia. " +
                       "Comment puis-je vous aider avec nos formations ?";
            }
        }
        
        // Check for attempts to make the bot do non-formation tasks
        // Expanded list of off-topic patterns (only block these if clearly not about formations)
        String[] offTopicPatterns = {
            "write code", "generate code", "create a program", "make a script", "code for me",
            "translate this", "translate that", "summarize this", "summarize that",
            "explain physics", "solve math", "calculate", "what is 2+2",
            "tell me a joke", "write a story", "compose a poem", "sing a song",
            "what's the weather", "what time is it", "what date is it", "current news",
            "play a game", "search the web", "search internet", "google this"
        };
        
        // Expanded list of formation-related keywords (more permissive to allow questions)
        boolean isFormationRelated = lower.contains("formation") || lower.contains("formations") ||
                                     lower.contains("training") || lower.contains("trainings") ||
                                     lower.contains("cours") || lower.contains("apprentissage") ||
                                     lower.contains("apprendre") || lower.contains("learn") ||
                                     lower.contains("certificat") || lower.contains("certificate") ||
                                     lower.contains("skilora") || lower.contains("skilora tunisia") ||
                                     lower.contains("développement") || lower.contains("development") ||
                                     lower.contains("design") || lower.contains("marketing") ||
                                     lower.contains("data") || lower.contains("science") ||
                                     lower.contains("langue") || lower.contains("language") ||
                                     lower.contains("compétence") || lower.contains("skill") ||
                                     lower.contains("débutant") || lower.contains("beginner") ||
                                     lower.contains("intermédiaire") || lower.contains("intermediate") ||
                                     lower.contains("avancé") || lower.contains("advanced") ||
                                     lower.contains("disponible") || lower.contains("available") ||
                                     lower.contains("recommand") || lower.contains("recommend") ||
                                     lower.contains("quel") || lower.contains("which") || lower.contains("what") ||
                                     lower.contains("comment") || lower.contains("how") ||
                                     lower.contains("pourquoi") || lower.contains("why") ||
                                     lower.contains("combien") || lower.contains("how much") ||
                                     lower.contains("prix") || lower.contains("price") || lower.contains("cost") ||
                                     lower.contains("durée") || lower.contains("duration") ||
                                     lower.contains("niveau") || lower.contains("level") ||
                                     lower.contains("catégorie") || lower.contains("category") ||
                                     lower.contains("voir") || lower.contains("voir les") || lower.contains("see") ||
                                     lower.contains("liste") || lower.contains("list") ||
                                     lower.contains("donner") || lower.contains("give me") ||
                                     lower.contains("montrer") || lower.contains("show") ||
                                     lower.contains("parler") || lower.contains("talk about") ||
                                     lower.contains("expliquer") || lower.contains("explain") ||
                                     lower.contains("obtenir") || lower.contains("get") ||
                                     lower.contains("avoir") || lower.contains("have");
        
        // Only block if it's clearly off-topic AND not formation-related
        // If message doesn't contain formation keywords but also doesn't match off-topic patterns,
        // allow it through - let the AI decide based on the strict system prompt
        if (!isFormationRelated) {
            for (String pattern : offTopicPatterns) {
                if (lower.contains(pattern)) {
                    logger.warn("Off-topic request detected: {}", userMessage);
                    return "Je suis désolé, mais je suis un assistant formations uniquement. " +
                           "Je ne peux répondre qu'aux questions concernant les formations et certificats Skilora Tunisia. " +
                           "Comment puis-je vous aider à choisir la bonne formation ?";
                }
            }
        }
        
        // If message passes validation, allow it - the AI will enforce formation-only responses via system prompt
        
        // Check for offensive content (basic check)
        String[] offensiveWords = {
            "hate", "kill", "violence", "terrorism", "bomb", "weapon"
        };
        
        for (String word : offensiveWords) {
            if (lower.contains(word)) {
                logger.warn("Potentially offensive content detected: {}", userMessage);
                return "Je suis désolé, mais je ne peux répondre qu'aux questions concernant les formations Skilora Tunisia. " +
                       "Comment puis-je vous aider avec nos formations ?";
            }
        }
        
        // Check if message is too long (prevent abuse)
        if (userMessage.length() > 500) {
            logger.warn("Message too long: {} characters", userMessage.length());
            return "Votre message est trop long. Veuillez limiter votre question à 500 caractères maximum.";
        }
        
        return null; // Input is safe
    }
    
    /**
     * Get or create session ID for conversation memory
     */
    public String getSessionId() {
        // Generate a simple session ID (in a real app, this could be user-specific)
        return UUID.randomUUID().toString();
    }
    
    /**
     * Clear session memory (for new conversation)
     */
    public void clearSession(String sessionId) {
        sessionMemory.remove(sessionId);
        sessionLanguage.remove(sessionId);
    }
    
    /**
     * Send message to AI and get response with session memory
     */
    public String sendMessage(String userMessage, List<Training> formations) {
        return sendMessage(userMessage, formations, null);
    }
    
    /**
     * Send message to AI and get response with session memory
     */
    public String sendMessage(String userMessage, List<Training> formations, String sessionId) {
        logger.info("=== sendMessage called ===");
        logger.info("User message: {}", userMessage);
        logger.info("Formations count: {}", formations != null ? formations.size() : 0);
        logger.info("Session ID: {}", sessionId);
        
        // Critical check: Verify formations are actually provided
        if (formations == null) {
            logger.error("CRITICAL ERROR: Formations list is NULL!");
        } else if (formations.isEmpty()) {
            logger.error("CRITICAL ERROR: Formations list is EMPTY! The chatbot will not have formation data.");
        } else {
            logger.info("Formations list is valid with {} items", formations.size());
            // Log first few formation titles for verification
            for (int i = 0; i < Math.min(3, formations.size()); i++) {
                logger.info("  Formation {}: {} (ID: {})", i + 1, formations.get(i).getTitle(), formations.get(i).getId());
            }
        }
        
        // Check API key first
        logger.info("Checking API configuration...");
        logger.info("API Key is blank: {}", apiKey == null || apiKey.isBlank());
        logger.info("API Key length: {}", apiKey != null ? apiKey.length() : 0);
        if (apiKey != null && !apiKey.isBlank()) {
            logger.info("API Key starts with: {}", apiKey.length() > 7 ? apiKey.substring(0, 7) + "..." : "***");
        }
        logger.info("isConfigured() returns: {}", isConfigured());
        logger.info("API URL: {}", GROQ_API_URL);
        logger.info("Model: {}", MODEL);
        
        if (!isConfigured()) {
            logger.error("API is NOT configured! Groq API key is missing or blank.");
            logger.error("Please set GROQ_API_KEY environment variable, -Dgroq.api.key=YOUR_KEY, or add to application.properties");
            return "Désolé, le service d'assistance IA n'est pas configuré. Veuillez contacter le support.";
        }
        
        logger.info("API is configured, proceeding with request...");
        
        // Validate input and get error message if unsafe
        String validationError = validateInput(userMessage);
        if (validationError != null) {
            return validationError;
        }
        
        // Generate session ID if not provided
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = getSessionId();
        }
        
        // Detect language from user message
        String detectedLanguage = detectLanguage(userMessage);
        String currentLanguage = sessionLanguage.getOrDefault(sessionId, detectedLanguage);
        
        // Update language if it changed
        if (!currentLanguage.equals(detectedLanguage)) {
            // Use the most recent language
            currentLanguage = detectedLanguage;
        }
        sessionLanguage.put(sessionId, currentLanguage);
        
        try {
            // STEP 2: Simplified API call for debugging
            System.out.println("╔═══════════════════════════════════════════════════════════════╗");
            System.out.println("║                    STEP 2: CALLING GROQ API                  ║");
            System.out.println("╠═══════════════════════════════════════════════════════════════╣");
            System.out.println("║ Calling Groq API...                                           ║");
            System.out.println("╚═══════════════════════════════════════════════════════════════╝");
            logger.info("STEP 2: Calling Groq API...");
            
            // Build system prompt with formations data and language
            String systemPrompt = buildSystemPrompt(formations, currentLanguage);
            
            // Create request JSON
            JsonObject request = new JsonObject();
            request.addProperty("model", MODEL);
            
            JsonArray messages = new JsonArray();
            
            // System message
            JsonObject systemMsg = new JsonObject();
            systemMsg.addProperty("role", "system");
            systemMsg.addProperty("content", systemPrompt);
            messages.add(systemMsg);
            
            // Get conversation history for this session
            List<JsonObject> conversationHistory = sessionMemory.getOrDefault(sessionId, new ArrayList<>());
            
            // Add conversation history (last 10 messages to avoid token limit)
            int historySize = conversationHistory.size();
            int startIndex = Math.max(0, historySize - 10);
            for (int i = startIndex; i < historySize; i++) {
                messages.add(conversationHistory.get(i));
            }
            
            // User message
            JsonObject userMsg = new JsonObject();
            userMsg.addProperty("role", "user");
            userMsg.addProperty("content", userMessage);
            messages.add(userMsg);
            
            // Store user message in history
            conversationHistory.add(userMsg);
            
            request.add("messages", messages);
            request.addProperty("temperature", 0.7);
            request.addProperty("max_tokens", 2000); // Increased to ensure complete professional responses
            
            String requestBodyJson = gson.toJson(request);
            System.out.println("╔═══════════════════════════════════════════════════════════════╗");
            System.out.println("║                    REQUEST BODY                               ║");
            System.out.println("╠═══════════════════════════════════════════════════════════════╣");
            System.out.println("║ " + String.format("%-62s", requestBodyJson.length() > 62 ? requestBodyJson.substring(0, 59) + "..." : requestBodyJson) + "║");
            System.out.println("╚═══════════════════════════════════════════════════════════════╝");
            logger.info("Request body: {}", requestBodyJson);
            
            // Send HTTP request
            URL url = new URL(GROQ_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            String authHeader = "Bearer " + apiKey;
            conn.setRequestProperty("Authorization", authHeader);
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            
            // Write request body
            byte[] requestBytes = requestBodyJson.getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(requestBytes, 0, requestBytes.length);
            }
            
            // Read response
            int responseCode = conn.getResponseCode();
            System.out.println("╔═══════════════════════════════════════════════════════════════╗");
            System.out.println("║                    RESPONSE CODE                              ║");
            System.out.println("╠═══════════════════════════════════════════════════════════════╣");
            System.out.println("║ Response Code: " + String.format("%-48s", responseCode) + "║");
            System.out.println("╚═══════════════════════════════════════════════════════════════╝");
            logger.info("Response code: {}", responseCode);
            
            String responseBody;
            if (responseCode == 200) {
                // Read response bytes directly and convert to UTF-8 String
                // This ensures proper handling of UTF-8 characters (é, à, etc.)
                try (InputStream is = conn.getInputStream()) {
                    byte[] responseBytes = is.readAllBytes();
                    responseBody = new String(responseBytes, StandardCharsets.UTF_8);
                    logger.debug("Response body read: {} bytes, UTF-8 string length: {}", 
                                responseBytes.length, responseBody.length());
                } catch (IOException e) {
                    logger.error("Error reading response stream", e);
                    responseBody = "Error reading response: " + e.getMessage();
                }
            } else {
                InputStream errorStream = conn.getErrorStream();
                if (errorStream != null) {
                    try {
                        byte[] errorBytes = errorStream.readAllBytes();
                        responseBody = new String(errorBytes, StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        logger.error("Error reading error stream", e);
                        responseBody = "Error reading error stream: " + e.getMessage();
                    }
                } else {
                    responseBody = "No error stream available";
                }
            }
            
            // STEP 3: Print full response
            System.out.println("╔═══════════════════════════════════════════════════════════════╗");
            System.out.println("║                    STEP 3: GROQ RESPONSE                     ║");
            System.out.println("╠═══════════════════════════════════════════════════════════════╣");
            System.out.println("║ Groq response: " + String.format("%-48s", responseBody.length() > 48 ? responseBody.substring(0, 45) + "..." : responseBody) + "║");
            System.out.println("╚═══════════════════════════════════════════════════════════════╝");
            logger.info("STEP 3: Groq response: {}", responseBody);
            System.out.println("Full Groq response JSON: " + responseBody);
            
            if (responseCode == 200) {
                // Parse JSON response - Gson handles UTF-8 correctly by default
                JsonObject response = gson.fromJson(responseBody, JsonObject.class);
                JsonArray choices = response.getAsJsonArray("choices");
                
                if (choices != null && choices.size() > 0) {
                    JsonObject choice = choices.get(0).getAsJsonObject();
                    JsonObject message = choice.getAsJsonObject("message");
                    
                    // Extract content - Gson preserves UTF-8 encoding
                    String content = message.get("content").getAsString();
                    
                    // Verify UTF-8 encoding is correct
                    // The content should already be in UTF-8 from Gson parsing
                    // But we'll ensure it's properly encoded for JavaFX display
                    logger.debug("Extracted content length: {}, contains é: {}, contains à: {}", 
                                content.length(), content.contains("é"), content.contains("à"));
                    
                    // Store assistant response in conversation history
                    JsonObject assistantMsg = new JsonObject();
                    assistantMsg.addProperty("role", "assistant");
                    assistantMsg.addProperty("content", content);
                    conversationHistory.add(assistantMsg);
                    
                    // Update session memory (keep last 20 messages)
                    if (conversationHistory.size() > 20) {
                        conversationHistory = conversationHistory.subList(conversationHistory.size() - 20, conversationHistory.size());
                    }
                    sessionMemory.put(sessionId, conversationHistory);
                    
                    return content;
                } else {
                    logger.error("No choices found in response");
                    return "Désolé, je n'ai pas pu générer de réponse. Veuillez réessayer.";
                }
            } else {
                // STEP 3: Print error details
                System.err.println("╔═══════════════════════════════════════════════════════════════╗");
                System.err.println("║                    STEP 3: GROQ ERROR                         ║");
                System.err.println("╠═══════════════════════════════════════════════════════════════╣");
                System.err.println("║ Response Code: " + String.format("%-48s", responseCode) + "║");
                System.err.println("║ Response Body: " + String.format("%-47s", responseBody.length() > 47 ? responseBody.substring(0, 44) + "..." : responseBody) + "║");
                System.err.println("╚═══════════════════════════════════════════════════════════════╝");
                System.err.println("Groq API error - Response Code: " + responseCode);
                System.err.println("Groq API error - Response Body: " + responseBody);
                logger.error("STEP 3: Groq API error - Response Code: {}, Body: {}", responseCode, responseBody);
                
                // Error messages in detected language
                if ("ar".equals(currentLanguage)) {
                    return "عذراً، حدث خطأ. يرجى المحاولة مرة أخرى لاحقاً.";
                } else if ("en".equals(currentLanguage)) {
                    return "Sorry, an error occurred. Please try again later.";
                } else {
                    return "Désolé, une erreur s'est produite. Veuillez réessayer plus tard.";
                }
            }
            
        } catch (IOException e) {
            // STEP 3: Print error
            System.err.println("╔═══════════════════════════════════════════════════════════════╗");
            System.err.println("║                    STEP 3: GROQ FETCH ERROR                  ║");
            System.err.println("╠═══════════════════════════════════════════════════════════════╣");
            System.err.println("║ Groq fetch error: " + String.format("%-45s", e.getMessage() != null ? (e.getMessage().length() > 45 ? e.getMessage().substring(0, 42) + "..." : e.getMessage()) : "null") + "║");
            System.err.println("╚═══════════════════════════════════════════════════════════════╝");
            System.err.println("Groq fetch error: " + e.getMessage());
            logger.error("STEP 3: Groq fetch error: {}", e.getMessage(), e);
            // CRITICAL ERROR LOG - Equivalent to console.error with all details
            System.err.println("╔═══════════════════════════════════════════════════════════════╗");
            System.err.println("║              GROQ ERROR DETAILS (IOException)                 ║");
            System.err.println("╠═══════════════════════════════════════════════════════════════╣");
            System.err.println("║ Exception Type: " + String.format("%-48s", e.getClass().getName()) + "║");
            System.err.println("║ Exception Message: " + String.format("%-45s", e.getMessage() != null ? (e.getMessage().length() > 45 ? e.getMessage().substring(0, 42) + "..." : e.getMessage()) : "null") + "║");
            if (e.getCause() != null) {
                System.err.println("║ Cause: " + String.format("%-54s", e.getCause().getMessage() != null ? (e.getCause().getMessage().length() > 54 ? e.getCause().getMessage().substring(0, 51) + "..." : e.getCause().getMessage()) : "null") + "║");
            }
            System.err.println("║ URL: " + String.format("%-56s", GROQ_API_URL) + "║");
            System.err.println("║ API Key Present: " + String.format("%-49s", String.valueOf(!apiKey.isBlank())) + "║");
            System.err.println("╚═══════════════════════════════════════════════════════════════╝");
            
            logger.error("==========================================");
            logger.error("DEBUG: GROQ API FULL ERROR (IOException)");
            logger.error("==========================================");
            logger.error("Exception type: {}", e.getClass().getName());
            logger.error("Exception message: {}", e.getMessage());
            if (e.getCause() != null) {
                logger.error("Cause type: {}", e.getCause().getClass().getName());
                logger.error("Cause message: {}", e.getCause().getMessage());
            }
            logger.error("Request URL was: {}", GROQ_API_URL);
            logger.error("API Key was present: {} (length: {})", !apiKey.isBlank(), apiKey.length());
            logger.error("Full stack trace:");
            for (StackTraceElement element : e.getStackTrace()) {
                logger.error("  at {}.{}({}:{})", 
                           element.getClassName(), 
                           element.getMethodName(), 
                           element.getFileName(), 
                           element.getLineNumber());
            }
            logger.error("GROQ ERROR DETAILS - Exception: {}, Message: {}, Cause: {}", 
                       e.getClass().getName(), e.getMessage(), 
                       e.getCause() != null ? e.getCause().getMessage() : "none");
            logger.error("==========================================");
            String errorLanguage = sessionLanguage.getOrDefault(sessionId, "fr");
            if ("ar".equals(errorLanguage)) {
                return "عذراً، لا يمكنني الاتصال بخدمة المساعدة. يرجى المحاولة مرة أخرى لاحقاً.";
            } else if ("en".equals(errorLanguage)) {
                return "Sorry, I cannot connect to the assistance service. Please try again later.";
            } else {
                return "Désolé, je ne peux pas me connecter au service d'assistance. Veuillez réessayer plus tard.";
            }
        } catch (Exception e) {
            // CRITICAL ERROR LOG - Equivalent to console.error with all details
            System.err.println("╔═══════════════════════════════════════════════════════════════╗");
            System.err.println("║         GROQ ERROR DETAILS (Unexpected Exception)             ║");
            System.err.println("╠═══════════════════════════════════════════════════════════════╣");
            System.err.println("║ Exception Type: " + String.format("%-48s", e.getClass().getName()) + "║");
            System.err.println("║ Exception Message: " + String.format("%-45s", e.getMessage() != null ? (e.getMessage().length() > 45 ? e.getMessage().substring(0, 42) + "..." : e.getMessage()) : "null") + "║");
            System.err.println("╚═══════════════════════════════════════════════════════════════╝");
            
            logger.error("==========================================");
            logger.error("DEBUG: GROQ API FULL ERROR (Unexpected Exception)");
            logger.error("==========================================");
            logger.error("Exception type: {}", e.getClass().getName());
            logger.error("Exception message: {}", e.getMessage());
            logger.error("GROQ ERROR DETAILS - Exception: {}, Message: {}", e.getClass().getName(), e.getMessage());
            logger.error("Stack trace:", e);
            logger.error("==========================================");
            String errorLanguage = sessionLanguage.getOrDefault(sessionId, "fr");
            if ("ar".equals(errorLanguage)) {
                return "حدث خطأ غير متوقع. يرجى المحاولة مرة أخرى.";
            } else if ("en".equals(errorLanguage)) {
                return "An unexpected error occurred. Please try again.";
            } else {
                return "Une erreur inattendue s'est produite. Veuillez réessayer.";
            }
        }
    }
}
