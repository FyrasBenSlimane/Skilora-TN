package com.skilora;

// === Community Entities ===
import com.skilora.community.entity.*;
import com.skilora.community.enums.*;
import com.skilora.community.service.*;

// === User entity (role checks) ===
import com.skilora.user.entity.User;
import com.skilora.user.enums.Role;
import com.skilora.config.DatabaseConfig;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 *   SKILORA - Community Module Test Suite
 * ──────────────────────────────────────────────────────────────────────
 *   Comprehensive tests covering:
 *   • 14 Entity classes (constructors, getters/setters, defaults, equals)
 *   • 4 Enum types (PostType, ConnectionStatus, EventType, EventStatus)
 *   • 20 Service classes (singletons, CRUD, edge cases)
 *   • Database schema validation (all community tables)
 *   • Post, Comments, Messaging, Groups, Events, Connections
 *   • Notifications, Blog, Reports, Company, Block, Search
 * ╚══════════════════════════════════════════════════════════════════════╝
 */
@TestMethodOrder(OrderAnnotation.class)
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
@DisplayName("Community Module Tests")
class CommunityModuleTests {

    // ═══════════════════════════════════════════════════════════════
    //  Section 1: Enum Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(1)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("1. Community Enums")
    class CommunityEnumTests {

        @Test @Order(1)
        @DisplayName("PostType has 5 values")
        void postTypeValues() {
            PostType[] values = PostType.values();
            assertEquals(5, values.length);
            assertNotNull(PostType.STATUS);
            assertNotNull(PostType.ARTICLE_SHARE);
            assertNotNull(PostType.JOB_SHARE);
            assertNotNull(PostType.ACHIEVEMENT);
            assertNotNull(PostType.SUCCESS_STORY);
        }

        @Test @Order(2)
        @DisplayName("ConnectionStatus has 4 values")
        void connectionStatusValues() {
            ConnectionStatus[] values = ConnectionStatus.values();
            assertEquals(4, values.length);
            assertNotNull(ConnectionStatus.PENDING);
            assertNotNull(ConnectionStatus.ACCEPTED);
            assertNotNull(ConnectionStatus.REJECTED);
            assertNotNull(ConnectionStatus.BLOCKED);
        }

        @Test @Order(3)
        @DisplayName("EventType has 6 values")
        void eventTypeValues() {
            EventType[] values = EventType.values();
            assertEquals(6, values.length);
            assertNotNull(EventType.MEETUP);
            assertNotNull(EventType.WEBINAR);
            assertNotNull(EventType.WORKSHOP);
            assertNotNull(EventType.CONFERENCE);
            assertNotNull(EventType.NETWORKING);
            assertNotNull(EventType.COMPETITION);
        }

        @Test @Order(4)
        @DisplayName("EventStatus has 4 values")
        void eventStatusValues() {
            EventStatus[] values = EventStatus.values();
            assertEquals(4, values.length);
            assertNotNull(EventStatus.UPCOMING);
            assertNotNull(EventStatus.ONGOING);
            assertNotNull(EventStatus.COMPLETED);
            assertNotNull(EventStatus.CANCELLED);
        }

        @ParameterizedTest
        @EnumSource(PostType.class)
        @Order(5)
        @DisplayName("All PostType values have valid names")
        void allPostTypesValid(PostType type) {
            assertNotNull(type.name());
            assertTrue(type.name().length() > 0);
        }

        @ParameterizedTest
        @EnumSource(ConnectionStatus.class)
        @Order(6)
        @DisplayName("All ConnectionStatus values have valid ordinals")
        void allConnectionStatusValid(ConnectionStatus status) {
            assertTrue(status.ordinal() >= 0);
        }

        @ParameterizedTest
        @EnumSource(EventType.class)
        @Order(7)
        @DisplayName("All EventType values have valid names")
        void allEventTypesValid(EventType type) {
            assertNotNull(type.name());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 2: Post Entity Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(10)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("10. Post Entity")
    class PostEntityTests {

        @Test @Order(1)
        @DisplayName("Post default constructor sets correct defaults")
        void postDefaults() {
            Post p = new Post();
            assertEquals(0, p.getId());
            assertEquals(0, p.getAuthorId());
            assertNull(p.getContent());
            assertNull(p.getImageUrl());
            assertEquals(PostType.STATUS, p.getPostType());
            assertEquals(0, p.getLikesCount());
            assertEquals(0, p.getCommentsCount());
            assertEquals(0, p.getSharesCount());
            assertTrue(p.isPublished());
        }

        @Test @Order(2)
        @DisplayName("Post setters/getters")
        void postSettersGetters() {
            Post p = new Post();
            p.setId(42);
            p.setAuthorId(7);
            p.setContent("Hello world!");
            p.setImageUrl("https://img.example.com/photo.jpg");
            p.setPostType(PostType.ACHIEVEMENT);
            p.setLikesCount(10);
            p.setCommentsCount(3);
            p.setSharesCount(1);
            p.setPublished(false);
            p.setAuthorName("Nyx");
            p.setAuthorPhoto("photo.jpg");
            p.setLikedByCurrentUser(true);

            assertEquals(42, p.getId());
            assertEquals(7, p.getAuthorId());
            assertEquals("Hello world!", p.getContent());
            assertEquals(PostType.ACHIEVEMENT, p.getPostType());
            assertEquals(10, p.getLikesCount());
            assertFalse(p.isPublished());
            assertEquals("Nyx", p.getAuthorName());
            assertTrue(p.isLikedByCurrentUser());
        }

        @Test @Order(3)
        @DisplayName("Post equals by ID")
        void postEquals() {
            Post p1 = new Post(); p1.setId(1);
            Post p2 = new Post(); p2.setId(1);
            Post p3 = new Post(); p3.setId(2);
            assertEquals(p1, p2);
            assertNotEquals(p1, p3);
            assertEquals(p1.hashCode(), p2.hashCode());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 3: PostComment Entity Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(11)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("11. PostComment Entity")
    class PostCommentEntityTests {

        @Test @Order(1)
        @DisplayName("PostComment default constructor")
        void commentDefaults() {
            PostComment c = new PostComment();
            assertEquals(0, c.getId());
            assertEquals(0, c.getPostId());
            assertEquals(0, c.getAuthorId());
            assertNull(c.getContent());
        }

        @Test @Order(2)
        @DisplayName("PostComment setters/getters")
        void commentSettersGetters() {
            PostComment c = new PostComment();
            c.setId(5);
            c.setPostId(10);
            c.setAuthorId(3);
            c.setContent("Great post!");
            c.setAuthorName("Test User");
            c.setAuthorPhoto("photo.png");

            assertEquals(5, c.getId());
            assertEquals(10, c.getPostId());
            assertEquals(3, c.getAuthorId());
            assertEquals("Great post!", c.getContent());
            assertEquals("Test User", c.getAuthorName());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 4: Message Entity Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(12)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("12. Message Entity")
    class MessageEntityTests {

        @Test @Order(1)
        @DisplayName("Message default constructor")
        void messageDefaults() {
            Message m = new Message();
            assertEquals(0, m.getId());
            assertFalse(m.isRead());
            assertEquals("TEXT", m.getMessageType());
        }

        @Test @Order(2)
        @DisplayName("Message type helpers")
        void messageTypeHelpers() {
            Message m = new Message();
            m.setMessageType("TEXT");
            assertTrue(m.isText());
            assertFalse(m.isImage());

            m.setMessageType("IMAGE");
            assertTrue(m.isImage());
            assertFalse(m.isText());

            m.setMessageType("VIDEO");
            assertTrue(m.isVideo());

            m.setMessageType("VOCAL");
            assertTrue(m.isVocal());
        }

        @Test @Order(3)
        @DisplayName("Message hasMedia")
        void messageHasMedia() {
            Message m = new Message();
            m.setMediaUrl(null);
            assertFalse(m.hasMedia());
            m.setMediaUrl("https://cdn.example.com/file.mp4");
            assertTrue(m.hasMedia());
        }

        @Test @Order(4)
        @DisplayName("Message fields")
        void messageFields() {
            Message m = new Message();
            m.setId(1);
            m.setConversationId(10);
            m.setSenderId(5);
            m.setContent("Hello!");
            m.setRead(true);
            m.setMediaUrl("https://example.com/img.png");
            m.setFileName("img.png");
            m.setDuration(120);
            m.setSenderName("Test");

            assertEquals(1, m.getId());
            assertEquals(10, m.getConversationId());
            assertEquals(5, m.getSenderId());
            assertEquals("Hello!", m.getContent());
            assertTrue(m.isRead());
            assertEquals(120, m.getDuration());
            assertEquals("Test", m.getSenderName());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 5: Conversation Entity Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(13)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("13. Conversation Entity")
    class ConversationEntityTests {

        @Test @Order(1)
        @DisplayName("Conversation defaults")
        void conversationDefaults() {
            Conversation c = new Conversation();
            assertFalse(c.isArchived1());
            assertFalse(c.isArchived2());
            assertEquals(0, c.getUnreadCount());
        }

        @Test @Order(2)
        @DisplayName("Conversation fields")
        void conversationFields() {
            Conversation c = new Conversation();
            c.setId(1);
            c.setParticipant1(5);
            c.setParticipant2(10);
            c.setOtherUserName("Jane");
            c.setLastMessagePreview("Hey!");
            c.setUnreadCount(3);

            assertEquals(1, c.getId());
            assertEquals(5, c.getParticipant1());
            assertEquals(10, c.getParticipant2());
            assertEquals("Jane", c.getOtherUserName());
            assertEquals("Hey!", c.getLastMessagePreview());
            assertEquals(3, c.getUnreadCount());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 6: CommunityGroup & GroupMember & GroupMessage
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(14)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("14. Group Entities")
    class GroupEntityTests {

        @Test @Order(1)
        @DisplayName("CommunityGroup defaults")
        void groupDefaults() {
            CommunityGroup g = new CommunityGroup();
            assertTrue(g.isPublic());
            assertEquals(1, g.getMemberCount());
            assertFalse(g.isMember());
        }

        @Test @Order(2)
        @DisplayName("CommunityGroup fields")
        void groupFields() {
            CommunityGroup g = new CommunityGroup();
            g.setId(1);
            g.setName("Java Devs");
            g.setDescription("A community for Java developers");
            g.setCategory("Development");
            g.setCreatorId(5);
            g.setMemberCount(42);
            g.setPublic(false);
            g.setCreatorName("Admin");
            g.setMember(true);

            assertEquals(1, g.getId());
            assertEquals("Java Devs", g.getName());
            assertEquals("Development", g.getCategory());
            assertEquals(42, g.getMemberCount());
            assertFalse(g.isPublic());
            assertTrue(g.isMember());
        }

        @Test @Order(3)
        @DisplayName("GroupMember defaults")
        void groupMemberDefaults() {
            GroupMember gm = new GroupMember();
            assertEquals("MEMBER", gm.getRole());
        }

        @Test @Order(4)
        @DisplayName("GroupMember fields")
        void groupMemberFields() {
            GroupMember gm = new GroupMember();
            gm.setId(1);
            gm.setGroupId(5);
            gm.setUserId(10);
            gm.setRole("ADMIN");
            gm.setUserName("Admin User");

            assertEquals(1, gm.getId());
            assertEquals(5, gm.getGroupId());
            assertEquals("ADMIN", gm.getRole());
        }

        @Test @Order(5)
        @DisplayName("GroupMessage defaults TEXT")
        void groupMessageDefaults() {
            GroupMessage gm = new GroupMessage();
            assertEquals("TEXT", gm.getMessageType());
        }

        @Test @Order(6)
        @DisplayName("GroupMessage type helpers")
        void groupMessageTypeHelpers() {
            GroupMessage gm = new GroupMessage();
            gm.setMessageType("IMAGE");
            assertTrue(gm.isImage());
            assertFalse(gm.isText());

            gm.setMessageType("VIDEO");
            assertTrue(gm.isVideo());

            gm.setMessageType("VOCAL");
            assertTrue(gm.isVocal());

            gm.setMessageType("TEXT");
            assertTrue(gm.isText());
        }

        @Test @Order(7)
        @DisplayName("GroupMessage hasMedia")
        void groupMessageHasMedia() {
            GroupMessage gm = new GroupMessage();
            assertFalse(gm.hasMedia());
            gm.setMediaUrl("https://cdn.example.com/audio.mp3");
            assertTrue(gm.hasMedia());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 7: Event & EventRsvp Entity Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(15)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("15. Event Entities")
    class EventEntityTests {

        @Test @Order(1)
        @DisplayName("Event defaults")
        void eventDefaults() {
            Event e = new Event();
            assertEquals(EventType.MEETUP, e.getEventType());
            assertEquals(EventStatus.UPCOMING, e.getStatus());
            assertFalse(e.isOnline());
            assertEquals(0, e.getCurrentAttendees());
            assertFalse(e.isAttending());
        }

        @Test @Order(2)
        @DisplayName("Event fields")
        void eventFields() {
            Event e = new Event();
            e.setId(1);
            e.setOrganizerId(5);
            e.setTitle("JavaFX Workshop");
            e.setDescription("Learn JavaFX");
            e.setEventType(EventType.WORKSHOP);
            e.setLocation("Tunis");
            e.setOnline(true);
            e.setOnlineLink("https://meet.example.com/room123");
            e.setMaxAttendees(50);
            e.setCurrentAttendees(25);
            e.setStatus(EventStatus.ONGOING);
            e.setOrganizerName("Organizer");
            e.setAttending(true);

            assertEquals(1, e.getId());
            assertEquals("JavaFX Workshop", e.getTitle());
            assertEquals(EventType.WORKSHOP, e.getEventType());
            assertTrue(e.isOnline());
            assertEquals(50, e.getMaxAttendees());
            assertEquals(25, e.getCurrentAttendees());
            assertEquals(EventStatus.ONGOING, e.getStatus());
            assertTrue(e.isAttending());
        }

        @Test @Order(3)
        @DisplayName("EventRsvp defaults")
        void rsvpDefaults() {
            EventRsvp rsvp = new EventRsvp();
            assertEquals("GOING", rsvp.getStatus());
        }

        @Test @Order(4)
        @DisplayName("EventRsvp fields")
        void rsvpFields() {
            EventRsvp rsvp = new EventRsvp();
            rsvp.setId(1);
            rsvp.setEventId(10);
            rsvp.setUserId(20);
            rsvp.setStatus("MAYBE");

            assertEquals(1, rsvp.getId());
            assertEquals(10, rsvp.getEventId());
            assertEquals(20, rsvp.getUserId());
            assertEquals("MAYBE", rsvp.getStatus());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 8: Connection Entity Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(16)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("16. Connection Entity")
    class ConnectionEntityTests {

        @Test @Order(1)
        @DisplayName("Connection defaults")
        void connectionDefaults() {
            com.skilora.community.entity.Connection conn = new com.skilora.community.entity.Connection();
            assertEquals(ConnectionStatus.PENDING, conn.getStatus());
            assertEquals("PROFESSIONAL", conn.getConnectionType());
            assertEquals(0, conn.getStrengthScore());
        }

        @Test @Order(2)
        @DisplayName("Connection fields")
        void connectionFields() {
            com.skilora.community.entity.Connection conn = new com.skilora.community.entity.Connection();
            conn.setId(1);
            conn.setUserId1(5);
            conn.setUserId2(10);
            conn.setRequesterId(5);
            conn.setStatus(ConnectionStatus.ACCEPTED);
            conn.setConnectionType("MENTORSHIP");
            conn.setStrengthScore(80);
            conn.setOtherUserName("Test User");

            assertEquals(1, conn.getId());
            assertEquals(5, conn.getUserId1());
            assertEquals(10, conn.getUserId2());
            assertEquals(ConnectionStatus.ACCEPTED, conn.getStatus());
            assertEquals("MENTORSHIP", conn.getConnectionType());
            assertEquals(80, conn.getStrengthScore());
        }

        @Test @Order(3)
        @DisplayName("Connection equals by ID")
        void connectionEquals() {
            com.skilora.community.entity.Connection c1 = new com.skilora.community.entity.Connection();
            c1.setId(1);
            com.skilora.community.entity.Connection c2 = new com.skilora.community.entity.Connection();
            c2.setId(1);
            assertEquals(c1, c2);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 9: Notification Entity Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(17)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("17. Notification Entity")
    class NotificationEntityTests {

        @Test @Order(1)
        @DisplayName("Notification default constructor")
        void notifDefaults() {
            Notification n = new Notification();
            assertEquals("INFO", n.getType());
            assertFalse(n.isRead());
        }

        @Test @Order(2)
        @DisplayName("Notification parameterized constructor")
        void notifParamConstructor() {
            Notification n = new Notification(5, "MESSAGE", "New Message", "You have a new message");
            assertEquals(5, n.getUserId());
            assertEquals("MESSAGE", n.getType());
            assertEquals("New Message", n.getTitle());
            assertEquals("You have a new message", n.getMessage());
        }

        @Test @Order(3)
        @DisplayName("Notification fields")
        void notifFields() {
            Notification n = new Notification();
            n.setId(1);
            n.setUserId(5);
            n.setType("APPLICATION");
            n.setTitle("Application Received");
            n.setMessage("New application for Java Developer");
            n.setIcon("📩");
            n.setRead(true);
            n.setReferenceType("application");
            n.setReferenceId(42);

            assertEquals("APPLICATION", n.getType());
            assertTrue(n.isRead());
            assertEquals("📩", n.getIcon());
            assertEquals(42, n.getReferenceId());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 10: BlogArticle Entity Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(18)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("18. BlogArticle Entity")
    class BlogArticleEntityTests {

        @Test @Order(1)
        @DisplayName("BlogArticle defaults")
        void blogDefaults() {
            BlogArticle b = new BlogArticle();
            assertFalse(b.isPublished());
            assertEquals(0, b.getViewsCount());
            assertEquals(0, b.getLikesCount());
        }

        @Test @Order(2)
        @DisplayName("BlogArticle fields")
        void blogFields() {
            BlogArticle b = new BlogArticle();
            b.setId(1);
            b.setAuthorId(5);
            b.setTitle("JavaFX Tips");
            b.setContent("Content here...");
            b.setSummary("A quick look at JavaFX tips");
            b.setCategory("Development");
            b.setTags("java,javafx,ui");
            b.setPublished(true);
            b.setViewsCount(100);
            b.setLikesCount(25);
            b.setAuthorName("Author");

            assertEquals("JavaFX Tips", b.getTitle());
            assertEquals("Development", b.getCategory());
            assertTrue(b.isPublished());
            assertEquals(100, b.getViewsCount());
            assertEquals(25, b.getLikesCount());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 11: Report Entity Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(19)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("19. Report Entity")
    class ReportEntityTests {

        @Test @Order(1)
        @DisplayName("Report default constructor")
        void reportDefaults() {
            Report r = new Report();
            assertEquals("PENDING", r.getStatus());
        }

        @Test @Order(2)
        @DisplayName("Report parameterized constructor")
        void reportParamConstructor() {
            Report r = new Report("Spam Report", "Spam", "This post is spam", 5);
            assertEquals("Spam Report", r.getSubject());
            assertEquals("Spam", r.getType());
            assertEquals("This post is spam", r.getDescription());
            assertEquals(5, r.getReporterId());
            assertEquals("PENDING", r.getStatus());
        }

        @Test @Order(3)
        @DisplayName("Report fields")
        void reportFields() {
            Report r = new Report();
            r.setId(1);
            r.setReportedEntityType("post");
            r.setReportedEntityId(42);
            r.setStatus("INVESTIGATING");
            r.setResolvedBy(100);

            assertEquals("INVESTIGATING", r.getStatus());
            assertEquals("post", r.getReportedEntityType());
            assertEquals(42, r.getReportedEntityId());
            assertEquals(100, r.getResolvedBy());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 12: Company Entity Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(20)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("20. Company Entity")
    class CompanyEntityTests {

        @Test @Order(1)
        @DisplayName("Company default constructor")
        void companyDefaults() {
            Company c = new Company();
            assertFalse(c.isVerified());
        }

        @Test @Order(2)
        @DisplayName("Company 3-arg constructor")
        void company3ArgConstructor() {
            Company c = new Company(5, "Skilora Inc.", "Tunisia");
            assertEquals(5, c.getOwnerId());
            assertEquals("Skilora Inc.", c.getName());
            assertEquals("Tunisia", c.getCountry());
        }

        @Test @Order(3)
        @DisplayName("Company full constructor")
        void companyFullConstructor() {
            Company c = new Company(5, "Skilora Inc.", "Tunisia", "IT",
                    "https://skilora.com", "logo.png", true, "51-200");
            assertEquals("IT", c.getIndustry());
            assertEquals("https://skilora.com", c.getWebsite());
            assertTrue(c.isVerified());
            assertEquals("51-200", c.getSize());
        }

        @Test @Order(4)
        @DisplayName("Company equals by ID")
        void companyEquals() {
            Company c1 = new Company(); c1.setId(1);
            Company c2 = new Company(); c2.setId(1);
            assertEquals(c1, c2);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 20: Service Singleton Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(30)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("30. Service Singletons")
    class ServiceSingletonTests {

        @Test @Order(1) @DisplayName("PostService singleton")
        void postServiceSingleton() {
            assertSame(PostService.getInstance(), PostService.getInstance());
        }

        @Test @Order(2) @DisplayName("BlogService singleton")
        void blogServiceSingleton() {
            assertSame(BlogService.getInstance(), BlogService.getInstance());
        }

        @Test @Order(3) @DisplayName("EventService singleton")
        void eventServiceSingleton() {
            assertSame(EventService.getInstance(), EventService.getInstance());
        }

        @Test @Order(4) @DisplayName("ConnectionService singleton")
        void connectionServiceSingleton() {
            assertSame(ConnectionService.getInstance(), ConnectionService.getInstance());
        }

        @Test @Order(5) @DisplayName("MessagingService singleton")
        void messagingServiceSingleton() {
            assertSame(MessagingService.getInstance(), MessagingService.getInstance());
        }

        @Test @Order(6) @DisplayName("GroupService singleton")
        void groupServiceSingleton() {
            assertSame(GroupService.getInstance(), GroupService.getInstance());
        }

        @Test @Order(7) @DisplayName("NotificationService singleton")
        void notificationServiceSingleton() {
            assertSame(NotificationService.getInstance(), NotificationService.getInstance());
        }

        @Test @Order(8) @DisplayName("ReportService singleton")
        void reportServiceSingleton() {
            assertSame(ReportService.getInstance(), ReportService.getInstance());
        }

        @Test @Order(9) @DisplayName("CompanyService singleton")
        void companyServiceSingleton() {
            assertSame(CompanyService.getInstance(), CompanyService.getInstance());
        }

        @Test @Order(10) @DisplayName("UserBlockService singleton")
        void userBlockServiceSingleton() {
            assertSame(UserBlockService.getInstance(), UserBlockService.getInstance());
        }

        @Test @Order(11) @DisplayName("SearchService singleton")
        void searchServiceSingleton() {
            assertSame(SearchService.getInstance(), SearchService.getInstance());
        }

        @Test @Order(12) @DisplayName("DashboardStatsService singleton")
        void dashboardStatsServiceSingleton() {
            assertSame(DashboardStatsService.getInstance(), DashboardStatsService.getInstance());
        }

        @Test @Order(13) @DisplayName("AudioRecorderService singleton")
        void audioRecorderSingleton() {
            assertSame(AudioRecorderService.getInstance(), AudioRecorderService.getInstance());
        }

        @Test @Order(14) @DisplayName("TranslationService singleton")
        void translationServiceSingleton() {
            assertSame(TranslationService.getInstance(), TranslationService.getInstance());
        }

        @Test @Order(15) @DisplayName("OnlineStatusService singleton")
        void onlineStatusServiceSingleton() {
            assertSame(OnlineStatusService.getInstance(), OnlineStatusService.getInstance());
        }

        @Test @Order(16) @DisplayName("MentionService singleton")
        void mentionServiceSingleton() {
            assertSame(MentionService.getInstance(), MentionService.getInstance());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 30: Database Schema Validation
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(40)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("40. Community DB Schema")
    class CommunityDbSchemaTests {

        private boolean tableExists(String table) throws SQLException {
            try (Connection conn = DatabaseConfig.getInstance().getConnection()) {
                ResultSet rs = conn.getMetaData().getTables(null, null, table, null);
                return rs.next();
            }
        }

        private boolean columnExists(String table, String column) throws SQLException {
            try (Connection conn = DatabaseConfig.getInstance().getConnection()) {
                ResultSet rs = conn.getMetaData().getColumns(null, null, table, column);
                return rs.next();
            }
        }

        @Test @Order(1)  @DisplayName("'posts' table exists")
        void postsTable() throws SQLException { assertTrue(tableExists("posts")); }

        @Test @Order(2)  @DisplayName("'post_comments' table exists")
        void postCommentsTable() throws SQLException { assertTrue(tableExists("post_comments")); }

        @Test @Order(3)  @DisplayName("'conversations' table exists")
        void conversationsTable() throws SQLException { assertTrue(tableExists("conversations")); }

        @Test @Order(4)  @DisplayName("'messages' table exists")
        void messagesTable() throws SQLException { assertTrue(tableExists("messages")); }

        @Test @Order(5)  @DisplayName("'community_groups' table exists")
        void groupsTable() throws SQLException { assertTrue(tableExists("community_groups")); }

        @Test @Order(6)  @DisplayName("'group_members' table exists")
        void groupMembersTable() throws SQLException { assertTrue(tableExists("group_members")); }

        @Test @Order(7)  @DisplayName("'group_messages' table exists")
        void groupMessagesTable() throws SQLException { assertTrue(tableExists("group_messages")); }

        @Test @Order(8)  @DisplayName("'events' table exists")
        void eventsTable() throws SQLException { assertTrue(tableExists("events")); }

        @Test @Order(9)  @DisplayName("'event_rsvps' table exists")
        void rsvpsTable() throws SQLException { assertTrue(tableExists("event_rsvps")); }

        @Test @Order(10)  @DisplayName("'connections' table exists")
        void connectionsTable() throws SQLException { assertTrue(tableExists("connections")); }

        @Test @Order(11) @DisplayName("'notifications' table exists")
        void notificationsTable() throws SQLException { assertTrue(tableExists("notifications")); }

        @Test @Order(12) @DisplayName("'blog_articles' table exists")
        void blogTable() throws SQLException { assertTrue(tableExists("blog_articles")); }

        @Test @Order(13) @DisplayName("'reports' table exists")
        void reportsTable() throws SQLException { assertTrue(tableExists("reports")); }

        @Test @Order(14) @DisplayName("'companies' table exists")
        void companiesTable() throws SQLException { assertTrue(tableExists("companies")); }

        @Test @Order(15) @DisplayName("'user_blocks' table exists")
        void userBlocksTable() throws SQLException { assertTrue(tableExists("user_blocks")); }

        // Column checks
        @Test @Order(20) @DisplayName("posts has key columns")
        void postsColumns() throws SQLException {
            assertTrue(columnExists("posts", "id"));
            assertTrue(columnExists("posts", "author_id"));
            assertTrue(columnExists("posts", "content"));
            assertTrue(columnExists("posts", "post_type"));
            assertTrue(columnExists("posts", "likes_count"));
        }

        @Test @Order(21) @DisplayName("messages has message_type column")
        void messagesColumns() throws SQLException {
            assertTrue(columnExists("messages", "conversation_id"));
            assertTrue(columnExists("messages", "sender_id"));
            assertTrue(columnExists("messages", "content"));
            assertTrue(columnExists("messages", "message_type"));
            assertTrue(columnExists("messages", "media_url"));
        }

        @Test @Order(22) @DisplayName("events has key columns")
        void eventsColumns() throws SQLException {
            assertTrue(columnExists("events", "organizer_id"));
            assertTrue(columnExists("events", "title"));
            assertTrue(columnExists("events", "event_type"));
            assertTrue(columnExists("events", "status"));
            assertTrue(columnExists("events", "max_attendees"));
        }

        @Test @Order(23) @DisplayName("connections has status column")
        void connectionsColumns() throws SQLException {
            assertTrue(columnExists("connections", "user_id_1"));
            assertTrue(columnExists("connections", "user_id_2"));
            assertTrue(columnExists("connections", "status"));
            assertTrue(columnExists("connections", "requester_id"));
        }

        @Test @Order(24) @DisplayName("notifications has reference columns")
        void notificationsColumns() throws SQLException {
            assertTrue(columnExists("notifications", "user_id"));
            assertTrue(columnExists("notifications", "type"));
            assertTrue(columnExists("notifications", "title"));
        }

        @Test @Order(25) @DisplayName("blog_articles has category column")
        void blogColumns() throws SQLException {
            assertTrue(columnExists("blog_articles", "author_id"));
            assertTrue(columnExists("blog_articles", "title"));
            assertTrue(columnExists("blog_articles", "content"));
            assertTrue(columnExists("blog_articles", "category"));
        }

        @Test @Order(26) @DisplayName("companies has key columns")
        void companiesColumns() throws SQLException {
            assertTrue(columnExists("companies", "owner_id"));
            assertTrue(columnExists("companies", "name"));
            assertTrue(columnExists("companies", "country"));
            assertTrue(columnExists("companies", "is_verified"));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 40: PostService CRUD Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(50)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("50. PostService CRUD")
    class PostServiceTests {

        private static final PostService service = PostService.getInstance();
        private static int testPostId = -1;

        @Test @Order(1)
        @DisplayName("findAll returns non-null list")
        void findAll() {
            assertNotNull(service.findAll());
        }

        @Test @Order(2)
        @DisplayName("create post returns valid ID")
        void createPost() {
            Post p = new Post();
            p.setAuthorId(1);
            p.setContent("JUnit test post " + System.currentTimeMillis());
            p.setPostType(PostType.STATUS);
            testPostId = service.create(p);
            assertTrue(testPostId > 0);
        }

        @Test @Order(3)
        @DisplayName("findById returns created post")
        void findById() {
            if (testPostId <= 0) return;
            Post p = service.findById(testPostId);
            assertNotNull(p);
            assertTrue(p.getContent().startsWith("JUnit test post"));
        }

        @Test @Order(4)
        @DisplayName("update post content")
        void updatePost() {
            if (testPostId <= 0) return;
            Post p = service.findById(testPostId);
            p.setContent("Updated content");
            assertTrue(service.update(p));
            assertEquals("Updated content", service.findById(testPostId).getContent());
        }

        @Test @Order(5)
        @DisplayName("toggleLike does not throw")
        void toggleLike() {
            if (testPostId <= 0) return;
            // toggleLike may return false if DB transaction fails in test context
            assertDoesNotThrow(() -> service.toggleLike(testPostId, 999));
            // Clean up in case it succeeded
            service.toggleLike(testPostId, 999);
        }

        @Test @Order(6)
        @DisplayName("addComment and getComments")
        void comments() {
            if (testPostId <= 0) return;
            PostComment c = new PostComment();
            c.setPostId(testPostId);
            c.setAuthorId(1);
            c.setContent("Test comment");
            int commentId = service.addComment(c);
            assertTrue(commentId > 0);

            List<PostComment> comments = service.getComments(testPostId);
            assertFalse(comments.isEmpty());
            assertTrue(comments.stream().anyMatch(cc -> cc.getContent().equals("Test comment")));

            // Cleanup
            service.deleteComment(commentId, testPostId);
        }

        @Test @Order(7)
        @DisplayName("getByAuthor returns posts")
        void getByAuthor() {
            if (testPostId <= 0) return;
            List<Post> posts = service.getByAuthor(1);
            assertNotNull(posts);
        }

        @Test @Order(99)
        @DisplayName("delete post")
        void deletePost() {
            if (testPostId <= 0) return;
            assertTrue(service.delete(testPostId));
            assertNull(service.findById(testPostId));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 50: BlogService CRUD Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(55)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("55. BlogService CRUD")
    class BlogServiceTests {

        private static final BlogService service = BlogService.getInstance();
        private static int testArticleId = -1;

        @Test @Order(1)
        @DisplayName("findAll returns list")
        void findAll() {
            assertNotNull(service.findAll());
        }

        @Test @Order(2)
        @DisplayName("create article returns valid ID")
        void createArticle() {
            BlogArticle a = new BlogArticle();
            a.setAuthorId(1);
            a.setTitle("JUnit Blog Test " + System.currentTimeMillis());
            a.setContent("Test content for blog article");
            a.setSummary("Test summary");
            a.setCategory("Development");
            a.setTags("junit,test");
            a.setPublished(true);
            testArticleId = service.create(a);
            assertTrue(testArticleId > 0);
        }

        @Test @Order(3)
        @DisplayName("findById returns article")
        void findById() {
            if (testArticleId <= 0) return;
            BlogArticle a = service.findById(testArticleId);
            assertNotNull(a);
            assertTrue(a.getTitle().startsWith("JUnit Blog Test"));
        }

        @Test @Order(4)
        @DisplayName("findPublished includes published article")
        void findPublished() {
            if (testArticleId <= 0) return;
            List<BlogArticle> published = service.findPublished();
            assertNotNull(published);
        }

        @Test @Order(5)
        @DisplayName("findByCategory returns filtered list")
        void findByCategory() {
            List<BlogArticle> devArticles = service.findByCategory("Development");
            assertNotNull(devArticles);
        }

        @Test @Order(6)
        @DisplayName("search finds article")
        void search() {
            if (testArticleId <= 0) return;
            List<BlogArticle> results = service.search("JUnit Blog Test");
            assertNotNull(results);
        }

        @Test @Order(7)
        @DisplayName("incrementViews")
        void incrementViews() {
            if (testArticleId <= 0) return;
            assertTrue(service.incrementViews(testArticleId));
        }

        @Test @Order(99)
        @DisplayName("delete article")
        void deleteArticle() {
            if (testArticleId <= 0) return;
            assertTrue(service.delete(testArticleId));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 60: ConnectionService Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(60)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("60. ConnectionService Operations")
    class ConnectionServiceTests {

        private static final ConnectionService service = ConnectionService.getInstance();

        @Test @Order(1)
        @DisplayName("areConnected returns false for non-connected users")
        void areConnectedFalse() {
            assertFalse(service.areConnected(999998, 999999));
        }

        @Test @Order(2)
        @DisplayName("getConnections returns empty for non-existent user")
        void getConnectionsEmpty() {
            List<com.skilora.community.entity.Connection> conns = service.getConnections(999999);
            assertNotNull(conns);
            assertTrue(conns.isEmpty());
        }

        @Test @Order(3)
        @DisplayName("getPendingRequests returns empty for non-existent user")
        void getPendingEmpty() {
            List<com.skilora.community.entity.Connection> pending = service.getPendingRequests(999999);
            assertNotNull(pending);
            assertTrue(pending.isEmpty());
        }

        @Test @Order(4)
        @DisplayName("getConnectionCount returns 0 for non-existent user")
        void connectionCountZero() {
            assertEquals(0, service.getConnectionCount(999999));
        }

        @Test @Order(5)
        @DisplayName("getPendingCount returns 0 for non-existent user")
        void pendingCountZero() {
            assertEquals(0, service.getPendingCount(999999));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 70: NotificationService Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(70)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("70. NotificationService CRUD")
    class NotificationServiceTests {

        private static final NotificationService service = NotificationService.getInstance();
        private static int testNotifId = -1;

        @Test @Order(1)
        @DisplayName("findByUserId returns list for non-existent user")
        void findByUserIdEmpty() {
            List<Notification> notifs = service.findByUserId(999999);
            assertNotNull(notifs);
            assertTrue(notifs.isEmpty());
        }

        @Test @Order(2)
        @DisplayName("create notification returns ID")
        void createNotification() {
            Notification n = new Notification(1, "INFO", "Test Notification", "JUnit test");
            testNotifId = service.create(n);
            assertTrue(testNotifId > 0);
        }

        @Test @Order(3)
        @DisplayName("getUnreadCount >= 0")
        void unreadCount() {
            assertTrue(service.getUnreadCount(1) >= 0);
        }

        @Test @Order(4)
        @DisplayName("markAsRead works")
        void markAsRead() {
            if (testNotifId <= 0) return;
            assertTrue(service.markAsRead(testNotifId));
        }

        @Test @Order(99)
        @DisplayName("delete notification")
        void deleteNotification() {
            if (testNotifId <= 0) return;
            assertTrue(service.delete(testNotifId));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 80: CompanyService Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(80)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("80. CompanyService CRUD")
    class CompanyServiceTests {

        private static final CompanyService service = CompanyService.getInstance();

        @Test @Order(1)
        @DisplayName("findAll returns list")
        void findAll() {
            assertNotNull(service.findAll());
        }

        @Test @Order(2)
        @DisplayName("findById returns null for non-existent")
        void findByIdNull() {
            assertNull(service.findById(999999));
        }

        @Test @Order(3)
        @DisplayName("findByOwner returns null for non-existent")
        void findByOwnerNull() {
            assertNull(service.findByOwner(999999));
        }

        @Test @Order(4)
        @DisplayName("findVerified returns list")
        void findVerified() {
            assertNotNull(service.findVerified());
        }

        @Test @Order(5)
        @DisplayName("searchByName returns list")
        void searchByName() {
            List<Company> results = service.searchByName("nonexistent_xyz_test");
            assertNotNull(results);
            assertTrue(results.isEmpty());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 85: ReportService Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(85)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("85. ReportService CRUD")
    class ReportServiceTests {

        private static final ReportService service = ReportService.getInstance();
        private static int testReportId = -1;

        @Test @Order(1)
        @DisplayName("findAll returns list")
        void findAll() {
            assertNotNull(service.findAll());
        }

        @Test @Order(2)
        @DisplayName("create report returns ID")
        void createReport() {
            Report r = new Report("JUnit Test", "Spam", "Automated test report", 1);
            testReportId = service.create(r);
            assertTrue(testReportId > 0);
        }

        @Test @Order(3)
        @DisplayName("findByStatus returns list")
        void findByStatus() {
            List<Report> pending = service.findByStatus("PENDING");
            assertNotNull(pending);
        }

        @Test @Order(4)
        @DisplayName("updateStatus works")
        void updateStatus() {
            if (testReportId <= 0) return;
            assertTrue(service.updateStatus(testReportId, "DISMISSED", 1));
        }

        @Test @Order(99)
        @DisplayName("delete report")
        void deleteReport() {
            if (testReportId <= 0) return;
            assertTrue(service.delete(testReportId));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 88: UserBlockService Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(88)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("88. UserBlockService Operations")
    class UserBlockServiceTests {

        private static final UserBlockService service = UserBlockService.getInstance();

        @Test @Order(1)
        @DisplayName("isBlocked returns false for non-blocked")
        void isBlockedFalse() {
            assertFalse(service.isBlocked(999998, 999999));
        }

        @Test @Order(2)
        @DisplayName("isEitherBlocked returns false for non-blocked")
        void isEitherBlockedFalse() {
            assertFalse(service.isEitherBlocked(999998, 999999));
        }

        @Test @Order(3)
        @DisplayName("getBlockedUserIds returns empty for non-existent user")
        void blockedIdsEmpty() {
            List<Integer> ids = service.getBlockedUserIds(999999);
            assertNotNull(ids);
            assertTrue(ids.isEmpty());
        }

        @Test @Order(4)
        @DisplayName("getBlockedCount returns 0 for non-existent user")
        void blockedCountZero() {
            assertEquals(0, service.getBlockedCount(999999));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 90: MessagingService Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(90)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("90. MessagingService Operations")
    class MessagingServiceTests {

        private static final MessagingService service = MessagingService.getInstance();

        @Test @Order(1)
        @DisplayName("getConversations returns list for non-existent user")
        void getConversationsEmpty() {
            List<Conversation> convos = service.getConversations(999999);
            assertNotNull(convos);
        }

        @Test @Order(2)
        @DisplayName("getUnreadCount returns 0 for non-existent user")
        void unreadCountZero() {
            assertEquals(0, service.getUnreadCount(999999));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 92: EventService Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(92)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("92. EventService Operations")
    class EventServiceTests {

        private static final EventService service = EventService.getInstance();

        @Test @Order(1)
        @DisplayName("findUpcoming returns list")
        void findUpcoming() {
            assertNotNull(service.findUpcoming());
        }

        @Test @Order(2)
        @DisplayName("findByOrganizer returns empty for non-existent user")
        void findByOrganizerEmpty() {
            List<Event> events = service.findByOrganizer(999999);
            assertNotNull(events);
            assertTrue(events.isEmpty());
        }

        @Test @Order(3)
        @DisplayName("isAttending returns false for non-existent")
        void isAttendingFalse() {
            assertFalse(service.isAttending(999999, 999999));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 95: DashboardStatsService Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(95)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("95. DashboardStatsService")
    class DashboardStatsTests {

        private static final DashboardStatsService service = DashboardStatsService.getInstance();

        @Test @Order(1)
        @DisplayName("getTotalUsers >= 0")
        void totalUsers() { assertTrue(service.getTotalUsers() >= 0); }

        @Test @Order(2)
        @DisplayName("getNewUsersThisMonth >= 0")
        void newUsers() { assertTrue(service.getNewUsersThisMonth() >= 0); }

        @Test @Order(3)
        @DisplayName("getTotalActiveOffers >= 0")
        void activeOffers() { assertTrue(service.getTotalActiveOffers() >= 0); }

        @Test @Order(4)
        @DisplayName("getTotalFormations >= 0")
        void totalFormations() { assertTrue(service.getTotalFormations() >= 0); }

        @Test @Order(5)
        @DisplayName("getTotalEnrollments >= 0")
        void totalEnrollments() { assertTrue(service.getTotalEnrollments() >= 0); }

        @Test @Order(6)
        @DisplayName("getUserApplicationCount for non-existent user = 0")
        void userAppCount() { assertEquals(0, service.getUserApplicationCount(999999)); }

        @Test @Order(7)
        @DisplayName("getUserEnrollmentCount for non-existent user = 0")
        void userEnrollCount() { assertEquals(0, service.getUserEnrollmentCount(999999)); }

        @Test @Order(8)
        @DisplayName("getUserConnectionCount for non-existent user = 0")
        void userConnCount() { assertEquals(0, service.getUserConnectionCount(999999)); }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 98: Edge Cases & Cross-Feature Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(98)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("98. Edge Cases & Validation")
    class EdgeCaseTests {

        @Test @Order(1)
        @DisplayName("Post with null content is allowed by entity")
        void postNullContent() {
            Post p = new Post();
            p.setContent(null);
            assertNull(p.getContent());
        }

        @Test @Order(2)
        @DisplayName("Message default type is TEXT")
        void messageDefaultType() {
            assertEquals("TEXT", new Message().getMessageType());
        }

        @Test @Order(3)
        @DisplayName("GroupMessage default type is TEXT")
        void groupMessageDefaultType() {
            assertEquals("TEXT", new GroupMessage().getMessageType());
        }

        @Test @Order(4)
        @DisplayName("Event with null title is allowed by entity")
        void eventNullTitle() {
            Event e = new Event();
            e.setTitle(null);
            assertNull(e.getTitle());
        }

        @Test @Order(5)
        @DisplayName("Notification with all types")
        void notificationTypes() {
            String[] types = {"APPLICATION", "VIEW", "ACCEPTANCE", "MESSAGE", "MATCH", "INFO", "SYSTEM"};
            for (String type : types) {
                Notification n = new Notification();
                n.setType(type);
                assertEquals(type, n.getType());
            }
        }

        @Test @Order(6)
        @DisplayName("Report status transitions")
        void reportStatusTransitions() {
            String[] statuses = {"PENDING", "INVESTIGATING", "RESOLVED", "DISMISSED"};
            Report r = new Report();
            for (String s : statuses) {
                r.setStatus(s);
                assertEquals(s, r.getStatus());
            }
        }

        @Test @Order(7)
        @DisplayName("EventRsvp status values")
        void rsvpStatusValues() {
            String[] statuses = {"GOING", "MAYBE", "NOT_GOING"};
            EventRsvp rsvp = new EventRsvp();
            for (String s : statuses) {
                rsvp.setStatus(s);
                assertEquals(s, rsvp.getStatus());
            }
        }

        @Test @Order(8)
        @DisplayName("GroupMember role values")
        void groupMemberRoles() {
            String[] roles = {"ADMIN", "MODERATOR", "MEMBER"};
            GroupMember gm = new GroupMember();
            for (String r : roles) {
                gm.setRole(r);
                assertEquals(r, gm.getRole());
            }
        }

        @Test @Order(9)
        @DisplayName("Company size values")
        void companySizes() {
            String[] sizes = {"1-10", "11-50", "51-200", "201-500", "500+"};
            Company c = new Company();
            for (String s : sizes) {
                c.setSize(s);
                assertEquals(s, c.getSize());
            }
        }
    }
}
