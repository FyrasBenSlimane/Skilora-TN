package com.skilora;

import com.skilora.config.DatabaseConfig;
import com.skilora.support.entity.*;
import com.skilora.support.enums.*;
import com.skilora.support.service.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 *   SKILORA - Support Module Test Suite
 *   Tests: Entities, Enums, DB Connection, Service CRUD,
 *          Ticket lifecycle, Messages, FAQ, AutoResponse,
 *          Chatbot, Feedback
 * ╚══════════════════════════════════════════════════════════════╝
 */
@TestMethodOrder(OrderAnnotation.class)
@DisplayName("Support Module Tests")
class SupportTests {

    // ═══════════════════════════════════════════════════════════
    //  SECTION 1: DATABASE CONNECTION
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("1. Database Connection")
    @TestMethodOrder(OrderAnnotation.class)
    class DatabaseConnectionTests {

        @Test
        @Order(1)
        @DisplayName("DatabaseConfig singleton returns same instance")
        void testSingleton() {
            DatabaseConfig db1 = DatabaseConfig.getInstance();
            DatabaseConfig db2 = DatabaseConfig.getInstance();
            assertSame(db1, db2);
        }

        @Test
        @Order(2)
        @DisplayName("Database connection is valid")
        void testConnection() throws SQLException {
            Connection conn = DatabaseConfig.getInstance().getConnection();
            assertNotNull(conn, "Connection should not be null");
            assertFalse(conn.isClosed(), "Connection should be open");
            conn.close();
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  SECTION 2: ENUMS
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("2. Support Enums")
    @TestMethodOrder(OrderAnnotation.class)
    class EnumTests {

        @Test
        @Order(1)
        @DisplayName("TicketStatus enum has 5 values")
        void testTicketStatusValues() {
            TicketStatus[] values = TicketStatus.values();
            assertEquals(5, values.length);
            assertNotNull(TicketStatus.OPEN);
            assertNotNull(TicketStatus.IN_PROGRESS);
            assertNotNull(TicketStatus.WAITING_REPLY);
            assertNotNull(TicketStatus.RESOLVED);
            assertNotNull(TicketStatus.CLOSED);
        }

        @Test
        @Order(2)
        @DisplayName("TicketStatus valueOf works")
        void testTicketStatusValueOf() {
            assertEquals(TicketStatus.OPEN, TicketStatus.valueOf("OPEN"));
            assertEquals(TicketStatus.CLOSED, TicketStatus.valueOf("CLOSED"));
        }

        @Test
        @Order(3)
        @DisplayName("TicketPriority enum has 4 values")
        void testTicketPriorityValues() {
            TicketPriority[] values = TicketPriority.values();
            assertEquals(4, values.length);
            assertNotNull(TicketPriority.LOW);
            assertNotNull(TicketPriority.MEDIUM);
            assertNotNull(TicketPriority.HIGH);
            assertNotNull(TicketPriority.URGENT);
        }

        @Test
        @Order(4)
        @DisplayName("TicketPriority valueOf works")
        void testTicketPriorityValueOf() {
            assertEquals(TicketPriority.URGENT, TicketPriority.valueOf("URGENT"));
        }

        @Test
        @Order(5)
        @DisplayName("FeedbackType enum has 5 values")
        void testFeedbackTypeValues() {
            FeedbackType[] values = FeedbackType.values();
            assertEquals(5, values.length);
            assertNotNull(FeedbackType.BUG_REPORT);
            assertNotNull(FeedbackType.FEATURE_REQUEST);
            assertNotNull(FeedbackType.GENERAL);
            assertNotNull(FeedbackType.COMPLAINT);
            assertNotNull(FeedbackType.PRAISE);
        }

        @Test
        @Order(6)
        @DisplayName("FeedbackType valueOf works")
        void testFeedbackTypeValueOf() {
            assertEquals(FeedbackType.BUG_REPORT, FeedbackType.valueOf("BUG_REPORT"));
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  SECTION 3: ENTITY CONSTRUCTORS & GETTERS/SETTERS
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("3. SupportTicket Entity")
    @TestMethodOrder(OrderAnnotation.class)
    class SupportTicketEntityTests {

        @Test
        @Order(1)
        @DisplayName("Default constructor creates empty ticket")
        void testDefaultConstructor() {
            SupportTicket ticket = new SupportTicket();
            assertEquals(0, ticket.getId());
            assertEquals(0, ticket.getUserId());
            assertNull(ticket.getSubject());
            assertNull(ticket.getDescription());
            assertNull(ticket.getStatus());
        }

        @Test
        @Order(2)
        @DisplayName("Parameterized constructor sets fields correctly")
        void testParamConstructor() {
            SupportTicket ticket = new SupportTicket(1, "TECHNICAL", "HIGH", "OPEN",
                    "Test Subject", "Test Description");
            assertEquals(1, ticket.getUserId());
            assertEquals("TECHNICAL", ticket.getCategory());
            assertEquals("HIGH", ticket.getPriority());
            assertEquals("OPEN", ticket.getStatus());
            assertEquals("Test Subject", ticket.getSubject());
            assertEquals("Test Description", ticket.getDescription());
        }

        @Test
        @Order(3)
        @DisplayName("All getters and setters work")
        void testGettersSetters() {
            SupportTicket ticket = new SupportTicket();
            ticket.setId(42);
            ticket.setUserId(7);
            ticket.setCategory("BILLING");
            ticket.setPriority("URGENT");
            ticket.setStatus("IN_PROGRESS");
            ticket.setSubject("Invoice issue");
            ticket.setDescription("Cannot download invoice");
            ticket.setAssignedTo(3);

            LocalDateTime now = LocalDateTime.now();
            ticket.setCreatedDate(now);
            ticket.setUpdatedDate(now);
            ticket.setResolvedDate(now);
            ticket.setUserName("John Doe");
            ticket.setAssignedToName("Admin User");

            assertEquals(42, ticket.getId());
            assertEquals(7, ticket.getUserId());
            assertEquals("BILLING", ticket.getCategory());
            assertEquals("URGENT", ticket.getPriority());
            assertEquals("IN_PROGRESS", ticket.getStatus());
            assertEquals("Invoice issue", ticket.getSubject());
            assertEquals("Cannot download invoice", ticket.getDescription());
            assertEquals(3, ticket.getAssignedTo());
            assertEquals(now, ticket.getCreatedDate());
            assertEquals(now, ticket.getUpdatedDate());
            assertEquals(now, ticket.getResolvedDate());
            assertEquals("John Doe", ticket.getUserName());
            assertEquals("Admin User", ticket.getAssignedToName());
        }

        @Test
        @Order(4)
        @DisplayName("equals and hashCode work correctly")
        void testEqualsHashCode() {
            SupportTicket t1 = new SupportTicket();
            t1.setId(1);
            SupportTicket t2 = new SupportTicket();
            t2.setId(1);
            SupportTicket t3 = new SupportTicket();
            t3.setId(2);

            assertEquals(t1, t2);
            assertNotEquals(t1, t3);
            assertEquals(t1.hashCode(), t2.hashCode());
        }

        @Test
        @Order(5)
        @DisplayName("toString returns non-null string")
        void testToString() {
            SupportTicket ticket = new SupportTicket(1, "TECH", "HIGH", "OPEN", "Test", "Desc");
            String str = ticket.toString();
            assertNotNull(str);
            assertFalse(str.isEmpty());
        }
    }

    @Nested
    @DisplayName("4. TicketMessage Entity")
    @TestMethodOrder(OrderAnnotation.class)
    class TicketMessageEntityTests {

        @Test
        @Order(1)
        @DisplayName("Default constructor creates empty message")
        void testDefaultConstructor() {
            TicketMessage msg = new TicketMessage();
            assertEquals(0, msg.getId());
            assertEquals(0, msg.getTicketId());
            assertNull(msg.getMessage());
            assertFalse(msg.isInternal());
        }

        @Test
        @Order(2)
        @DisplayName("Parameterized constructor sets fields correctly")
        void testParamConstructor() {
            TicketMessage msg = new TicketMessage(10, 5, "Hello", true);
            assertEquals(10, msg.getTicketId());
            assertEquals(5, msg.getSenderId());
            assertEquals("Hello", msg.getMessage());
            assertTrue(msg.isInternal());
        }

        @Test
        @Order(3)
        @DisplayName("All getters and setters work")
        void testGettersSetters() {
            TicketMessage msg = new TicketMessage();
            msg.setId(99);
            msg.setTicketId(10);
            msg.setSenderId(5);
            msg.setMessage("Test message");
            msg.setInternal(true);

            LocalDateTime now = LocalDateTime.now();
            msg.setCreatedDate(now);
            msg.setSenderName("Admin");
            msg.setSenderRole("ADMIN");

            assertEquals(99, msg.getId());
            assertEquals(10, msg.getTicketId());
            assertEquals(5, msg.getSenderId());
            assertEquals("Test message", msg.getMessage());
            assertTrue(msg.isInternal());
            assertEquals(now, msg.getCreatedDate());
            assertEquals("Admin", msg.getSenderName());
            assertEquals("ADMIN", msg.getSenderRole());
        }

        @Test
        @Order(4)
        @DisplayName("equals and hashCode")
        void testEqualsHashCode() {
            TicketMessage m1 = new TicketMessage();
            m1.setId(1);
            TicketMessage m2 = new TicketMessage();
            m2.setId(1);
            assertEquals(m1, m2);
            assertEquals(m1.hashCode(), m2.hashCode());
        }
    }

    @Nested
    @DisplayName("5. FAQArticle Entity")
    @TestMethodOrder(OrderAnnotation.class)
    class FAQArticleEntityTests {

        @Test
        @Order(1)
        @DisplayName("Default constructor creates empty article")
        void testDefaultConstructor() {
            FAQArticle article = new FAQArticle();
            assertEquals(0, article.getId());
            assertNull(article.getQuestion());
            assertNull(article.getAnswer());
        }

        @Test
        @Order(2)
        @DisplayName("Parameterized constructor sets fields and isPublished=true")
        void testParamConstructor() {
            FAQArticle article = new FAQArticle("General", "How to reset?", "Click reset.", "en");
            assertEquals("General", article.getCategory());
            assertEquals("How to reset?", article.getQuestion());
            assertEquals("Click reset.", article.getAnswer());
            assertEquals("en", article.getLanguage());
            assertTrue(article.isPublished());
        }

        @Test
        @Order(3)
        @DisplayName("All getters and setters work")
        void testGettersSetters() {
            FAQArticle article = new FAQArticle();
            article.setId(5);
            article.setCategory("Account");
            article.setQuestion("How to delete account?");
            article.setAnswer("Go to settings.");
            article.setLanguage("fr");
            article.setHelpfulCount(10);
            article.setViewCount(100);
            article.setPublished(false);

            LocalDateTime now = LocalDateTime.now();
            article.setCreatedDate(now);
            article.setUpdatedDate(now);

            assertEquals(5, article.getId());
            assertEquals("Account", article.getCategory());
            assertEquals("How to delete account?", article.getQuestion());
            assertEquals("Go to settings.", article.getAnswer());
            assertEquals("fr", article.getLanguage());
            assertEquals(10, article.getHelpfulCount());
            assertEquals(100, article.getViewCount());
            assertFalse(article.isPublished());
            assertEquals(now, article.getCreatedDate());
            assertEquals(now, article.getUpdatedDate());
        }

        @Test
        @Order(4)
        @DisplayName("equals and hashCode")
        void testEqualsHashCode() {
            FAQArticle a1 = new FAQArticle();
            a1.setId(1);
            FAQArticle a2 = new FAQArticle();
            a2.setId(1);
            assertEquals(a1, a2);
            assertEquals(a1.hashCode(), a2.hashCode());
        }
    }

    @Nested
    @DisplayName("6. AutoResponse Entity")
    @TestMethodOrder(OrderAnnotation.class)
    class AutoResponseEntityTests {

        @Test
        @Order(1)
        @DisplayName("Default constructor creates empty auto-response")
        void testDefaultConstructor() {
            AutoResponse ar = new AutoResponse();
            assertEquals(0, ar.getId());
            assertNull(ar.getTriggerKeyword());
            assertNull(ar.getResponseText());
        }

        @Test
        @Order(2)
        @DisplayName("Parameterized constructor sets fields and isActive=true")
        void testParamConstructor() {
            AutoResponse ar = new AutoResponse("password", "Reset via settings", "Account", "en");
            assertEquals("password", ar.getTriggerKeyword());
            assertEquals("Reset via settings", ar.getResponseText());
            assertEquals("Account", ar.getCategory());
            assertEquals("en", ar.getLanguage());
            assertTrue(ar.isActive());
        }

        @Test
        @Order(3)
        @DisplayName("All getters and setters work")
        void testGettersSetters() {
            AutoResponse ar = new AutoResponse();
            ar.setId(3);
            ar.setTriggerKeyword("invoice");
            ar.setResponseText("Check billing section");
            ar.setCategory("Billing");
            ar.setLanguage("fr");
            ar.setActive(false);
            ar.setUsageCount(42);

            LocalDateTime now = LocalDateTime.now();
            ar.setCreatedDate(now);

            assertEquals(3, ar.getId());
            assertEquals("invoice", ar.getTriggerKeyword());
            assertEquals("Check billing section", ar.getResponseText());
            assertEquals("Billing", ar.getCategory());
            assertEquals("fr", ar.getLanguage());
            assertFalse(ar.isActive());
            assertEquals(42, ar.getUsageCount());
            assertEquals(now, ar.getCreatedDate());
        }

        @Test
        @Order(4)
        @DisplayName("equals and hashCode")
        void testEqualsHashCode() {
            AutoResponse a1 = new AutoResponse();
            a1.setId(1);
            AutoResponse a2 = new AutoResponse();
            a2.setId(1);
            assertEquals(a1, a2);
            assertEquals(a1.hashCode(), a2.hashCode());
        }
    }

    @Nested
    @DisplayName("7. ChatbotConversation Entity")
    @TestMethodOrder(OrderAnnotation.class)
    class ChatbotConversationEntityTests {

        @Test
        @Order(1)
        @DisplayName("Default constructor creates empty conversation")
        void testDefaultConstructor() {
            ChatbotConversation conv = new ChatbotConversation();
            assertEquals(0, conv.getId());
            assertNull(conv.getStatus());
        }

        @Test
        @Order(2)
        @DisplayName("Parameterized constructor sets userId and status")
        void testParamConstructor() {
            ChatbotConversation conv = new ChatbotConversation(5, "ACTIVE");
            assertEquals(5, conv.getUserId());
            assertEquals("ACTIVE", conv.getStatus());
        }

        @Test
        @Order(3)
        @DisplayName("All getters and setters work")
        void testGettersSetters() {
            ChatbotConversation conv = new ChatbotConversation();
            conv.setId(10);
            conv.setUserId(7);
            conv.setStatus("ENDED");
            conv.setEscalatedToTicketId(42);

            LocalDateTime now = LocalDateTime.now();
            conv.setStartedDate(now);
            conv.setEndedDate(now);

            assertEquals(10, conv.getId());
            assertEquals(7, conv.getUserId());
            assertEquals("ENDED", conv.getStatus());
            assertEquals(42, conv.getEscalatedToTicketId());
            assertEquals(now, conv.getStartedDate());
            assertEquals(now, conv.getEndedDate());
        }

        @Test
        @Order(4)
        @DisplayName("equals and hashCode")
        void testEqualsHashCode() {
            ChatbotConversation c1 = new ChatbotConversation();
            c1.setId(1);
            ChatbotConversation c2 = new ChatbotConversation();
            c2.setId(1);
            assertEquals(c1, c2);
            assertEquals(c1.hashCode(), c2.hashCode());
        }
    }

    @Nested
    @DisplayName("8. ChatbotMessage Entity")
    @TestMethodOrder(OrderAnnotation.class)
    class ChatbotMessageEntityTests {

        @Test
        @Order(1)
        @DisplayName("Default constructor creates empty message")
        void testDefaultConstructor() {
            ChatbotMessage msg = new ChatbotMessage();
            assertEquals(0, msg.getId());
            assertNull(msg.getSender());
            assertNull(msg.getMessage());
        }

        @Test
        @Order(2)
        @DisplayName("Parameterized constructor sets fields")
        void testParamConstructor() {
            ChatbotMessage msg = new ChatbotMessage(3, "user", "Hello bot");
            assertEquals(3, msg.getConversationId());
            assertEquals("user", msg.getSender());
            assertEquals("Hello bot", msg.getMessage());
        }

        @Test
        @Order(3)
        @DisplayName("All getters and setters work")
        void testGettersSetters() {
            ChatbotMessage msg = new ChatbotMessage();
            msg.setId(55);
            msg.setConversationId(3);
            msg.setSender("bot");
            msg.setMessage("How can I help?");
            msg.setIntent("greeting");
            msg.setConfidence(0.95);

            LocalDateTime now = LocalDateTime.now();
            msg.setCreatedDate(now);

            assertEquals(55, msg.getId());
            assertEquals(3, msg.getConversationId());
            assertEquals("bot", msg.getSender());
            assertEquals("How can I help?", msg.getMessage());
            assertEquals("greeting", msg.getIntent());
            assertEquals(0.95, msg.getConfidence(), 0.001);
            assertEquals(now, msg.getCreatedDate());
        }

        @Test
        @Order(4)
        @DisplayName("equals and hashCode")
        void testEqualsHashCode() {
            ChatbotMessage m1 = new ChatbotMessage();
            m1.setId(1);
            ChatbotMessage m2 = new ChatbotMessage();
            m2.setId(1);
            assertEquals(m1, m2);
            assertEquals(m1.hashCode(), m2.hashCode());
        }
    }

    @Nested
    @DisplayName("9. UserFeedback Entity")
    @TestMethodOrder(OrderAnnotation.class)
    class UserFeedbackEntityTests {

        @Test
        @Order(1)
        @DisplayName("Default constructor creates empty feedback")
        void testDefaultConstructor() {
            UserFeedback fb = new UserFeedback();
            assertEquals(0, fb.getId());
            assertNull(fb.getComment());
            assertFalse(fb.isResolved());
        }

        @Test
        @Order(2)
        @DisplayName("Parameterized constructor sets fields")
        void testParamConstructor() {
            UserFeedback fb = new UserFeedback(1, "BUG_REPORT", 3, "UI glitch");
            assertEquals(1, fb.getUserId());
            assertEquals("BUG_REPORT", fb.getFeedbackType());
            assertEquals(3, fb.getRating());
            assertEquals("UI glitch", fb.getComment());
        }

        @Test
        @Order(3)
        @DisplayName("All getters and setters work")
        void testGettersSetters() {
            UserFeedback fb = new UserFeedback();
            fb.setId(10);
            fb.setUserId(3);
            fb.setFeedbackType("PRAISE");
            fb.setRating(5);
            fb.setComment("Great app!");
            fb.setCategory("General");
            fb.setResolved(true);
            fb.setUserName("Jane");

            LocalDateTime now = LocalDateTime.now();
            fb.setCreatedDate(now);

            assertEquals(10, fb.getId());
            assertEquals(3, fb.getUserId());
            assertEquals("PRAISE", fb.getFeedbackType());
            assertEquals(5, fb.getRating());
            assertEquals("Great app!", fb.getComment());
            assertEquals("General", fb.getCategory());
            assertTrue(fb.isResolved());
            assertEquals("Jane", fb.getUserName());
            assertEquals(now, fb.getCreatedDate());
        }

        @Test
        @Order(4)
        @DisplayName("equals and hashCode")
        void testEqualsHashCode() {
            UserFeedback f1 = new UserFeedback();
            f1.setId(1);
            UserFeedback f2 = new UserFeedback();
            f2.setId(1);
            assertEquals(f1, f2);
            assertEquals(f1.hashCode(), f2.hashCode());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  SECTION 4: SERVICE SINGLETONS
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("10. Service Singletons")
    @TestMethodOrder(OrderAnnotation.class)
    class ServiceSingletonTests {

        @Test
        @Order(1)
        @DisplayName("SupportTicketService singleton")
        void testTicketServiceSingleton() {
            SupportTicketService s1 = SupportTicketService.getInstance();
            SupportTicketService s2 = SupportTicketService.getInstance();
            assertNotNull(s1);
            assertSame(s1, s2);
        }

        @Test
        @Order(2)
        @DisplayName("TicketMessageService singleton")
        void testMessageServiceSingleton() {
            TicketMessageService s1 = TicketMessageService.getInstance();
            TicketMessageService s2 = TicketMessageService.getInstance();
            assertNotNull(s1);
            assertSame(s1, s2);
        }

        @Test
        @Order(3)
        @DisplayName("FAQService singleton")
        void testFAQServiceSingleton() {
            FAQService s1 = FAQService.getInstance();
            FAQService s2 = FAQService.getInstance();
            assertNotNull(s1);
            assertSame(s1, s2);
        }

        @Test
        @Order(4)
        @DisplayName("AutoResponseService singleton")
        void testAutoResponseServiceSingleton() {
            AutoResponseService s1 = AutoResponseService.getInstance();
            AutoResponseService s2 = AutoResponseService.getInstance();
            assertNotNull(s1);
            assertSame(s1, s2);
        }

        @Test
        @Order(5)
        @DisplayName("ChatbotService singleton")
        void testChatbotServiceSingleton() {
            ChatbotService s1 = ChatbotService.getInstance();
            ChatbotService s2 = ChatbotService.getInstance();
            assertNotNull(s1);
            assertSame(s1, s2);
        }

        @Test
        @Order(6)
        @DisplayName("UserFeedbackService singleton")
        void testFeedbackServiceSingleton() {
            UserFeedbackService s1 = UserFeedbackService.getInstance();
            UserFeedbackService s2 = UserFeedbackService.getInstance();
            assertNotNull(s1);
            assertSame(s1, s2);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  SECTION 5: SUPPORT TICKET SERVICE CRUD
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("11. SupportTicket Service CRUD")
    @TestMethodOrder(OrderAnnotation.class)
    class TicketServiceCRUDTests {

        static int createdTicketId;

        @Test
        @Order(1)
        @DisplayName("Create a support ticket")
        void testCreate() {
            SupportTicketService service = SupportTicketService.getInstance();
            SupportTicket ticket = new SupportTicket(1, "TECHNICAL", "HIGH", "OPEN",
                    "TEST_TICKET_" + System.currentTimeMillis(), "Automated test ticket description");
            int id = service.create(ticket);
            assertTrue(id > 0, "create() should return a positive ID");
            createdTicketId = id;
        }

        @Test
        @Order(2)
        @DisplayName("Find ticket by ID")
        void testFindById() {
            SupportTicketService service = SupportTicketService.getInstance();
            SupportTicket ticket = service.findById(createdTicketId);
            assertNotNull(ticket, "findById should return the created ticket");
            assertEquals(createdTicketId, ticket.getId());
            assertEquals("TECHNICAL", ticket.getCategory());
            assertEquals("HIGH", ticket.getPriority());
            assertEquals("OPEN", ticket.getStatus());
            assertTrue(ticket.getSubject().startsWith("TEST_TICKET_"));
        }

        @Test
        @Order(3)
        @DisplayName("Find all tickets returns non-null list")
        void testFindAll() {
            SupportTicketService service = SupportTicketService.getInstance();
            List<SupportTicket> tickets = service.findAll();
            assertNotNull(tickets);
            assertTrue(tickets.size() >= 1, "Should have at least the test ticket");
        }

        @Test
        @Order(4)
        @DisplayName("Find tickets by status")
        void testFindByStatus() {
            SupportTicketService service = SupportTicketService.getInstance();
            List<SupportTicket> tickets = service.findByStatus("OPEN");
            assertNotNull(tickets);
            for (SupportTicket t : tickets) {
                assertEquals("OPEN", t.getStatus());
            }
        }

        @Test
        @Order(5)
        @DisplayName("Find tickets by priority")
        void testFindByPriority() {
            SupportTicketService service = SupportTicketService.getInstance();
            List<SupportTicket> tickets = service.findByPriority("HIGH");
            assertNotNull(tickets);
            for (SupportTicket t : tickets) {
                assertEquals("HIGH", t.getPriority());
            }
        }

        @Test
        @Order(6)
        @DisplayName("Find tickets by user ID")
        void testFindByUserId() {
            SupportTicketService service = SupportTicketService.getInstance();
            List<SupportTicket> tickets = service.findByUserId(1);
            assertNotNull(tickets);
        }

        @Test
        @Order(7)
        @DisplayName("Update ticket status")
        void testUpdateStatus() {
            SupportTicketService service = SupportTicketService.getInstance();
            boolean updated = service.updateStatus(createdTicketId, "IN_PROGRESS");
            assertTrue(updated, "updateStatus should return true");

            SupportTicket ticket = service.findById(createdTicketId);
            assertEquals("IN_PROGRESS", ticket.getStatus());
        }

        @Test
        @Order(8)
        @DisplayName("Assign ticket to admin")
        void testAssign() {
            SupportTicketService service = SupportTicketService.getInstance();
            boolean assigned = service.assign(createdTicketId, 1);
            assertTrue(assigned, "assign should return true");

            SupportTicket ticket = service.findById(createdTicketId);
            assertEquals(1, ticket.getAssignedTo());
        }

        @Test
        @Order(9)
        @DisplayName("Update ticket fields")
        void testUpdate() {
            SupportTicketService service = SupportTicketService.getInstance();
            SupportTicket ticket = service.findById(createdTicketId);
            ticket.setSubject("UPDATED_TEST_TICKET");
            ticket.setPriority("URGENT");
            boolean updated = service.update(ticket);
            assertTrue(updated, "update should return true");

            SupportTicket refreshed = service.findById(createdTicketId);
            assertEquals("UPDATED_TEST_TICKET", refreshed.getSubject());
            assertEquals("URGENT", refreshed.getPriority());
        }

        @Test
        @Order(10)
        @DisplayName("Count open tickets")
        void testCountOpen() {
            SupportTicketService service = SupportTicketService.getInstance();
            long count = service.countOpen();
            assertTrue(count >= 0, "countOpen should return >= 0");
        }

        @Test
        @Order(11)
        @DisplayName("Count by status")
        void testCountByStatus() {
            SupportTicketService service = SupportTicketService.getInstance();
            long count = service.countByStatus("IN_PROGRESS");
            assertTrue(count >= 1, "Should have at least our test ticket IN_PROGRESS");
        }

        @Test
        @Order(12)
        @DisplayName("Delete ticket")
        void testDelete() {
            SupportTicketService service = SupportTicketService.getInstance();
            boolean deleted = service.delete(createdTicketId);
            assertTrue(deleted, "delete should return true");

            SupportTicket ticket = service.findById(createdTicketId);
            assertNull(ticket, "findById should return null after deletion");
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  SECTION 6: TICKET MESSAGE SERVICE CRUD
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("12. TicketMessage Service CRUD")
    @TestMethodOrder(OrderAnnotation.class)
    class MessageServiceCRUDTests {

        static int testTicketId;
        static int testMessageId;

        @Test
        @Order(1)
        @DisplayName("Setup: create a ticket for messages")
        void setupTicket() {
            SupportTicketService ticketService = SupportTicketService.getInstance();
            SupportTicket ticket = new SupportTicket(1, "TECHNICAL", "MEDIUM", "OPEN",
                    "MSG_TEST_" + System.currentTimeMillis(), "Testing messages");
            testTicketId = ticketService.create(ticket);
            assertTrue(testTicketId > 0);
        }

        @Test
        @Order(2)
        @DisplayName("Add message to ticket")
        void testAddMessage() {
            TicketMessageService service = TicketMessageService.getInstance();
            TicketMessage msg = new TicketMessage(testTicketId, 1, "This is a test reply", false);
            int id = service.addMessage(msg);
            assertTrue(id > 0, "addMessage should return a positive ID");
            testMessageId = id;
        }

        @Test
        @Order(3)
        @DisplayName("Add internal message to ticket")
        void testAddInternalMessage() {
            TicketMessageService service = TicketMessageService.getInstance();
            TicketMessage msg = new TicketMessage(testTicketId, 1, "Internal admin note", true);
            int id = service.addMessage(msg);
            assertTrue(id > 0);
        }

        @Test
        @Order(4)
        @DisplayName("Find messages by ticket ID")
        void testFindByTicketId() {
            TicketMessageService service = TicketMessageService.getInstance();
            List<TicketMessage> messages = service.findByTicketId(testTicketId);
            assertNotNull(messages);
            assertTrue(messages.size() >= 2, "Should have at least 2 test messages");
        }

        @Test
        @Order(5)
        @DisplayName("Delete message")
        void testDeleteMessage() {
            TicketMessageService service = TicketMessageService.getInstance();
            boolean deleted = service.delete(testMessageId);
            assertTrue(deleted, "delete should return true");
        }

        @Test
        @Order(6)
        @DisplayName("Cleanup: delete test ticket")
        void cleanup() {
            SupportTicketService.getInstance().delete(testTicketId);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  SECTION 7: FAQ SERVICE CRUD
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("13. FAQ Service CRUD")
    @TestMethodOrder(OrderAnnotation.class)
    class FAQServiceCRUDTests {

        static int testFaqId;

        @Test
        @Order(1)
        @DisplayName("Create FAQ article")
        void testCreate() {
            FAQService service = FAQService.getInstance();
            FAQArticle article = new FAQArticle("TEST_CAT", "TEST_Q_" + System.currentTimeMillis(),
                    "Test answer", "en");
            int id = service.create(article);
            assertTrue(id > 0, "create() should return a positive ID");
            testFaqId = id;
        }

        @Test
        @Order(2)
        @DisplayName("Find FAQ by ID")
        void testFindById() {
            FAQService service = FAQService.getInstance();
            FAQArticle article = service.findById(testFaqId);
            assertNotNull(article);
            assertEquals(testFaqId, article.getId());
            assertEquals("TEST_CAT", article.getCategory());
        }

        @Test
        @Order(3)
        @DisplayName("Find all FAQ articles")
        void testFindAll() {
            FAQService service = FAQService.getInstance();
            List<FAQArticle> articles = service.findAll();
            assertNotNull(articles);
            assertTrue(articles.size() >= 1);
        }

        @Test
        @Order(4)
        @DisplayName("Find FAQ by category")
        void testFindByCategory() {
            FAQService service = FAQService.getInstance();
            List<FAQArticle> articles = service.findByCategory("TEST_CAT");
            assertNotNull(articles);
            assertTrue(articles.size() >= 1);
        }

        @Test
        @Order(5)
        @DisplayName("Search FAQ articles")
        void testSearch() {
            FAQService service = FAQService.getInstance();
            List<FAQArticle> articles = service.search("TEST_Q");
            assertNotNull(articles);
            assertTrue(articles.size() >= 1);
        }

        @Test
        @Order(6)
        @DisplayName("Update FAQ article")
        void testUpdate() {
            FAQService service = FAQService.getInstance();
            FAQArticle article = service.findById(testFaqId);
            article.setAnswer("Updated answer");
            boolean updated = service.update(article);
            assertTrue(updated);

            FAQArticle refreshed = service.findById(testFaqId);
            assertEquals("Updated answer", refreshed.getAnswer());
        }

        @Test
        @Order(7)
        @DisplayName("Delete FAQ article")
        void testDelete() {
            FAQService service = FAQService.getInstance();
            boolean deleted = service.delete(testFaqId);
            assertTrue(deleted);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  SECTION 8: AUTO-RESPONSE SERVICE CRUD
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("14. AutoResponse Service CRUD")
    @TestMethodOrder(OrderAnnotation.class)
    class AutoResponseServiceCRUDTests {

        static int testAutoId;

        @Test
        @Order(1)
        @DisplayName("Create auto-response")
        void testCreate() {
            AutoResponseService service = AutoResponseService.getInstance();
            AutoResponse ar = new AutoResponse("TEST_KW_" + System.currentTimeMillis(),
                    "Test response text", "TEST_CAT", "en");
            int id = service.create(ar);
            assertTrue(id > 0, "create should return positive ID");
            testAutoId = id;
        }

        @Test
        @Order(2)
        @DisplayName("Find all auto-responses")
        void testFindAll() {
            AutoResponseService service = AutoResponseService.getInstance();
            List<AutoResponse> list = service.findAll();
            assertNotNull(list);
            assertTrue(list.size() >= 1);
        }

        @Test
        @Order(3)
        @DisplayName("Find active auto-responses")
        void testFindActive() {
            AutoResponseService service = AutoResponseService.getInstance();
            List<AutoResponse> list = service.findActive();
            assertNotNull(list);
            for (AutoResponse ar : list) {
                assertTrue(ar.isActive());
            }
        }

        @Test
        @Order(4)
        @DisplayName("Toggle auto-response active status")
        void testToggle() {
            AutoResponseService service = AutoResponseService.getInstance();
            boolean toggled = service.toggleActive(testAutoId);
            assertTrue(toggled);
        }

        @Test
        @Order(5)
        @DisplayName("Update auto-response")
        void testUpdate() {
            AutoResponseService service = AutoResponseService.getInstance();
            AutoResponse ar = new AutoResponse();
            ar.setId(testAutoId);
            ar.setTriggerKeyword("UPDATED_KW");
            ar.setResponseText("Updated response");
            ar.setCategory("UPDATED_CAT");
            ar.setLanguage("fr");
            boolean updated = service.update(ar);
            assertTrue(updated);
        }

        @Test
        @Order(6)
        @DisplayName("Delete auto-response")
        void testDelete() {
            AutoResponseService service = AutoResponseService.getInstance();
            boolean deleted = service.delete(testAutoId);
            assertTrue(deleted);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  SECTION 9: CHATBOT SERVICE
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("15. Chatbot Service")
    @TestMethodOrder(OrderAnnotation.class)
    class ChatbotServiceTests {

        static int testConversationId;

        @Test
        @Order(1)
        @DisplayName("Start a chatbot conversation")
        void testStartConversation() {
            ChatbotService service = ChatbotService.getInstance();
            int id = service.startConversation(1);
            assertTrue(id > 0, "startConversation should return positive ID");
            testConversationId = id;
        }

        @Test
        @Order(2)
        @DisplayName("Find conversation by ID")
        void testFindById() {
            ChatbotService service = ChatbotService.getInstance();
            ChatbotConversation conv = service.findById(testConversationId);
            assertNotNull(conv);
            assertEquals(testConversationId, conv.getId());
            assertEquals(1, conv.getUserId());
        }

        @Test
        @Order(3)
        @DisplayName("Find conversations by user ID")
        void testFindByUserId() {
            ChatbotService service = ChatbotService.getInstance();
            List<ChatbotConversation> list = service.findByUserId(1);
            assertNotNull(list);
            assertTrue(list.size() >= 1);
        }

        @Test
        @Order(4)
        @DisplayName("Add message to conversation")
        void testAddMessage() {
            ChatbotService service = ChatbotService.getInstance();
            ChatbotMessage msg = new ChatbotMessage(testConversationId, "user", "Test message");
            int id = service.addMessage(msg);
            assertTrue(id > 0);
        }

        @Test
        @Order(5)
        @DisplayName("Get messages from conversation")
        void testGetMessages() {
            ChatbotService service = ChatbotService.getInstance();
            List<ChatbotMessage> messages = service.getMessages(testConversationId);
            assertNotNull(messages);
            assertTrue(messages.size() >= 1);
        }

        @Test
        @Order(6)
        @DisplayName("Get auto-response for keyword")
        void testGetAutoResponse() {
            ChatbotService service = ChatbotService.getInstance();
            // May return null if no matching keyword, but should not throw
            service.getAutoResponse("nonexistent_keyword_xyz");
            // Just verify it doesn't throw — response may be null
        }

        @Test
        @Order(7)
        @DisplayName("End conversation")
        void testEndConversation() {
            ChatbotService service = ChatbotService.getInstance();
            boolean ended = service.endConversation(testConversationId);
            assertTrue(ended);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  SECTION 10: USER FEEDBACK SERVICE CRUD
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("16. UserFeedback Service CRUD")
    @TestMethodOrder(OrderAnnotation.class)
    class FeedbackServiceCRUDTests {

        static int testFeedbackId;

        @Test
        @Order(1)
        @DisplayName("Submit user feedback")
        void testSubmit() {
            UserFeedbackService service = UserFeedbackService.getInstance();
            UserFeedback fb = new UserFeedback(1, "BUG_REPORT", 3, "TEST_FB_" + System.currentTimeMillis());
            int id = service.submit(fb);
            assertTrue(id > 0, "submit should return positive ID");
            testFeedbackId = id;
        }

        @Test
        @Order(2)
        @DisplayName("Find all feedback")
        void testFindAll() {
            UserFeedbackService service = UserFeedbackService.getInstance();
            List<UserFeedback> list = service.findAll();
            assertNotNull(list);
            assertTrue(list.size() >= 1);
        }

        @Test
        @Order(3)
        @DisplayName("Find feedback by user ID")
        void testFindByUserId() {
            UserFeedbackService service = UserFeedbackService.getInstance();
            List<UserFeedback> list = service.findByUserId(1);
            assertNotNull(list);
            assertTrue(list.size() >= 1);
        }

        @Test
        @Order(4)
        @DisplayName("Find feedback by type")
        void testFindByType() {
            UserFeedbackService service = UserFeedbackService.getInstance();
            List<UserFeedback> list = service.findByType("BUG_REPORT");
            assertNotNull(list);
            for (UserFeedback fb : list) {
                assertEquals("BUG_REPORT", fb.getFeedbackType());
            }
        }

        @Test
        @Order(5)
        @DisplayName("Get average rating")
        void testGetAverageRating() {
            UserFeedbackService service = UserFeedbackService.getInstance();
            double avg = service.getAverageRating();
            assertTrue(avg >= 0.0 && avg <= 5.0, "Average should be between 0 and 5");
        }

        @Test
        @Order(6)
        @DisplayName("Resolve feedback")
        void testResolve() {
            UserFeedbackService service = UserFeedbackService.getInstance();
            boolean resolved = service.resolve(testFeedbackId);
            assertTrue(resolved);
        }

        @Test
        @Order(7)
        @DisplayName("Delete feedback")
        void testDelete() {
            UserFeedbackService service = UserFeedbackService.getInstance();
            boolean deleted = service.delete(testFeedbackId);
            assertTrue(deleted);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  SECTION 11: TICKET LIFECYCLE (full workflow)
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("17. Ticket Lifecycle")
    @TestMethodOrder(OrderAnnotation.class)
    class TicketLifecycleTests {

        static int lifecycleTicketId;

        @Test
        @Order(1)
        @DisplayName("Create ticket (OPEN)")
        void createOpen() {
            SupportTicketService service = SupportTicketService.getInstance();
            SupportTicket ticket = new SupportTicket(1, "BILLING", "MEDIUM", "OPEN",
                    "LIFECYCLE_" + System.currentTimeMillis(), "Lifecycle test");
            lifecycleTicketId = service.create(ticket);
            assertTrue(lifecycleTicketId > 0);

            SupportTicket saved = service.findById(lifecycleTicketId);
            assertEquals("OPEN", saved.getStatus());
        }

        @Test
        @Order(2)
        @DisplayName("Assign to admin")
        void assign() {
            SupportTicketService service = SupportTicketService.getInstance();
            assertTrue(service.assign(lifecycleTicketId, 1));
        }

        @Test
        @Order(3)
        @DisplayName("Transition OPEN → IN_PROGRESS")
        void toInProgress() {
            SupportTicketService service = SupportTicketService.getInstance();
            assertTrue(service.updateStatus(lifecycleTicketId, "IN_PROGRESS"));
            assertEquals("IN_PROGRESS", service.findById(lifecycleTicketId).getStatus());
        }

        @Test
        @Order(4)
        @DisplayName("Admin replies to ticket")
        void adminReply() {
            TicketMessageService msgService = TicketMessageService.getInstance();
            TicketMessage reply = new TicketMessage(lifecycleTicketId, 1, "Working on it", false);
            assertTrue(msgService.addMessage(reply) > 0);
        }

        @Test
        @Order(5)
        @DisplayName("Transition IN_PROGRESS → RESOLVED")
        void toResolved() {
            SupportTicketService service = SupportTicketService.getInstance();
            assertTrue(service.updateStatus(lifecycleTicketId, "RESOLVED"));
            assertEquals("RESOLVED", service.findById(lifecycleTicketId).getStatus());
        }

        @Test
        @Order(6)
        @DisplayName("Transition RESOLVED → CLOSED")
        void toClosed() {
            SupportTicketService service = SupportTicketService.getInstance();
            assertTrue(service.updateStatus(lifecycleTicketId, "CLOSED"));
            assertEquals("CLOSED", service.findById(lifecycleTicketId).getStatus());
        }

        @Test
        @Order(7)
        @DisplayName("Reopen closed ticket → OPEN")
        void reopen() {
            SupportTicketService service = SupportTicketService.getInstance();
            assertTrue(service.updateStatus(lifecycleTicketId, "OPEN"));
            assertEquals("OPEN", service.findById(lifecycleTicketId).getStatus());
        }

        @Test
        @Order(8)
        @DisplayName("Cleanup lifecycle ticket")
        void cleanup() {
            SupportTicketService.getInstance().delete(lifecycleTicketId);
        }
    }
}
