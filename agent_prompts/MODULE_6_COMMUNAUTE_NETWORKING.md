# MODULE 6: Communauté & Networking — Agent Prompt

> **Goal:** Implement the complete Communauté & Networking module from scratch. This module handles professional connections, posts/feed, messaging, events, mentorship, blog articles, groups, and achievements/gamification. Build everything: entities, services, controllers, views, DB schema, and i18n.

---

## CONTEXT — READ FIRST

### Project Architecture

- **Language:** Java 17, JavaFX 21, Maven
- **Database:** MySQL 8.0 via HikariCP connection pool
- **Pattern:** MVC (Model = entity + service, View = FXML, Controller = JavaFX controller)
- **NO DAO LAYER.** Services directly use `DatabaseConfig.getInstance().getConnection()` with PreparedStatement
- **Singleton pattern** on all services: `private static volatile XxxService instance; public static XxxService getInstance()`
- **I18n:** All UI strings use `I18n.get("key")` from `com.skilora.utils.I18n`. Resource bundles at `src/main/resources/com/skilora/i18n/messages_xx.properties`
- **Logging:** SLF4J via `LoggerFactory.getLogger(ClassName.class)`
- **UI Components:** Custom TL* components from `com.skilora.framework.components` (TLButton, TLCard, TLBadge, TLTextField, TLSelect, TLAlert, TLDialog, TLTextarea, TLAvatar, TLAccordion, etc.)
- **Async:** All DB calls from controllers use `javafx.concurrent.Task` with `Platform.runLater()` for UI updates

### Key File Locations

```
src/main/java/com/skilora/
├── config/DatabaseConfig.java          — DB connection singleton (HikariCP)
├── config/DatabaseInitializer.java     — Auto-creates tables on startup
├── model/entity/                       — Pure POJO entities (no JavaFX deps)
├── model/enums/                        — Enum types
├── model/service/                      — Service singletons (CRUD + business logic)
├── controller/                         — FXML controllers
├── ui/MainView.java                    — Main navigation (loads FXML views)
├── utils/I18n.java                     — Internationalization utility

src/main/resources/com/skilora/
├── i18n/messages_*.properties          — 4 resource bundles (default/fr/en/ar)
├── view/*.fxml                         — FXML view files
```

### Existing Patterns

Same entity, service, controller, FXML, and DatabaseInitializer patterns as Modules 2-5. Key rules:
- Entity: Pure POJO, no JavaFX imports, `com.skilora.model.entity`
- Service: Singleton, direct JDBC, `com.skilora.model.service`
- Controller: `Initializable`, async `Task`, `I18n.get()`, `com.skilora.controller`
- FXML: TL* components, VBox root, `src/main/resources/com/skilora/view/`

### What Already Exists

- `FeedController.java` + `FeedView.fxml` — this is a **JOB feed** (external job listings from JSON). It is NOT a social feed. Do NOT modify these. The community module will have its OWN social feed/posts.

---

## WHAT'S MISSING — BUILD EVERYTHING FROM SCRATCH

This is the largest module (10 entities in the spec). Implement in priority order — the most impactful features first.

---

## PHASE 1: Database Schema

### Step 1.1 — Add all tables to `DatabaseInitializer.java`

```sql
-- Professional Connections (like LinkedIn)
CREATE TABLE IF NOT EXISTS connections (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id_1 INT NOT NULL,
    user_id_2 INT NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    connection_type VARCHAR(30) DEFAULT 'PROFESSIONAL',
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_interaction DATETIME,
    strength_score INT DEFAULT 0,
    UNIQUE KEY uq_connection (user_id_1, user_id_2),
    FOREIGN KEY (user_id_1) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id_2) REFERENCES users(id) ON DELETE CASCADE
);

-- Social Posts
CREATE TABLE IF NOT EXISTS posts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    author_id INT NOT NULL,
    content TEXT NOT NULL,
    image_url TEXT,
    post_type VARCHAR(30) DEFAULT 'STATUS',
    likes_count INT DEFAULT 0,
    comments_count INT DEFAULT 0,
    shares_count INT DEFAULT 0,
    is_published BOOLEAN DEFAULT TRUE,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_date DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Post Comments
CREATE TABLE IF NOT EXISTS post_comments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    post_id INT NOT NULL,
    author_id INT NOT NULL,
    content TEXT NOT NULL,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Post Likes
CREATE TABLE IF NOT EXISTS post_likes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    post_id INT NOT NULL,
    user_id INT NOT NULL,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_post_like (post_id, user_id),
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Direct Messages
CREATE TABLE IF NOT EXISTS conversations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    participant_1 INT NOT NULL,
    participant_2 INT NOT NULL,
    last_message_date DATETIME,
    is_archived_1 BOOLEAN DEFAULT FALSE,
    is_archived_2 BOOLEAN DEFAULT FALSE,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_conversation (participant_1, participant_2),
    FOREIGN KEY (participant_1) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (participant_2) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS messages (
    id INT AUTO_INCREMENT PRIMARY KEY,
    conversation_id INT NOT NULL,
    sender_id INT NOT NULL,
    content TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Events
CREATE TABLE IF NOT EXISTS events (
    id INT AUTO_INCREMENT PRIMARY KEY,
    organizer_id INT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    event_type VARCHAR(30) DEFAULT 'MEETUP',
    location VARCHAR(255),
    is_online BOOLEAN DEFAULT FALSE,
    online_link TEXT,
    start_date DATETIME NOT NULL,
    end_date DATETIME,
    max_attendees INT DEFAULT 0,
    current_attendees INT DEFAULT 0,
    image_url TEXT,
    status VARCHAR(20) DEFAULT 'UPCOMING',
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (organizer_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS event_rsvps (
    id INT AUTO_INCREMENT PRIMARY KEY,
    event_id INT NOT NULL,
    user_id INT NOT NULL,
    status VARCHAR(20) DEFAULT 'GOING',
    rsvp_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_rsvp (event_id, user_id),
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Mentorship
CREATE TABLE IF NOT EXISTS mentorships (
    id INT AUTO_INCREMENT PRIMARY KEY,
    mentor_id INT NOT NULL,
    mentee_id INT NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    topic VARCHAR(200),
    goals TEXT,
    start_date DATE,
    end_date DATE,
    rating INT DEFAULT 0,
    feedback TEXT,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_mentorship (mentor_id, mentee_id),
    FOREIGN KEY (mentor_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (mentee_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Blog Articles
CREATE TABLE IF NOT EXISTS blog_articles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    author_id INT NOT NULL,
    title VARCHAR(300) NOT NULL,
    content TEXT NOT NULL,
    summary TEXT,
    cover_image_url TEXT,
    category VARCHAR(50),
    tags VARCHAR(500),
    views_count INT DEFAULT 0,
    likes_count INT DEFAULT 0,
    is_published BOOLEAN DEFAULT FALSE,
    published_date DATETIME,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_date DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Groups
CREATE TABLE IF NOT EXISTS community_groups (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    category VARCHAR(50),
    cover_image_url TEXT,
    creator_id INT NOT NULL,
    member_count INT DEFAULT 1,
    is_public BOOLEAN DEFAULT TRUE,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (creator_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS group_members (
    id INT AUTO_INCREMENT PRIMARY KEY,
    group_id INT NOT NULL,
    user_id INT NOT NULL,
    role VARCHAR(20) DEFAULT 'MEMBER',
    joined_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_group_member (group_id, user_id),
    FOREIGN KEY (group_id) REFERENCES community_groups(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Achievements / Gamification
CREATE TABLE IF NOT EXISTS achievements (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    badge_type VARCHAR(50) NOT NULL,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    icon_url TEXT,
    earned_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    rarity VARCHAR(20) DEFAULT 'COMMON',
    points INT DEFAULT 10,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

Add indexes:
```sql
CREATE INDEX idx_connections_user1 ON connections(user_id_1);
CREATE INDEX idx_connections_user2 ON connections(user_id_2);
CREATE INDEX idx_connections_status ON connections(status);
CREATE INDEX idx_posts_author ON posts(author_id);
CREATE INDEX idx_posts_date ON posts(created_date);
CREATE INDEX idx_post_comments_post ON post_comments(post_id);
CREATE INDEX idx_post_likes_post ON post_likes(post_id);
CREATE INDEX idx_conversations_p1 ON conversations(participant_1);
CREATE INDEX idx_conversations_p2 ON conversations(participant_2);
CREATE INDEX idx_messages_conv ON messages(conversation_id);
CREATE INDEX idx_messages_sender ON messages(sender_id);
CREATE INDEX idx_events_organizer ON events(organizer_id);
CREATE INDEX idx_events_date ON events(start_date);
CREATE INDEX idx_event_rsvps_event ON event_rsvps(event_id);
CREATE INDEX idx_mentorships_mentor ON mentorships(mentor_id);
CREATE INDEX idx_mentorships_mentee ON mentorships(mentee_id);
CREATE INDEX idx_blog_author ON blog_articles(author_id);
CREATE INDEX idx_blog_published ON blog_articles(is_published, published_date);
CREATE INDEX idx_group_members_group ON group_members(group_id);
CREATE INDEX idx_group_members_user ON group_members(user_id);
CREATE INDEX idx_achievements_user ON achievements(user_id);
```

---

## PHASE 2: Enums

### Step 2.1 — Create enums in `com.skilora.model.enums`

**ConnectionStatus.java:**
```java
public enum ConnectionStatus { PENDING, ACCEPTED, REJECTED, BLOCKED }
```

**PostType.java:**
```java
public enum PostType { STATUS, ARTICLE_SHARE, JOB_SHARE, ACHIEVEMENT, SUCCESS_STORY }
```

**EventType.java:**
```java
public enum EventType { MEETUP, WEBINAR, WORKSHOP, CONFERENCE, NETWORKING }
```

**EventStatus.java:**
```java
public enum EventStatus { UPCOMING, ONGOING, COMPLETED, CANCELLED }
```

**MentorshipStatus.java:**
```java
public enum MentorshipStatus { PENDING, ACTIVE, COMPLETED, CANCELLED }
```

**BadgeRarity.java:**
```java
public enum BadgeRarity { COMMON, UNCOMMON, RARE, EPIC, LEGENDARY }
```

---

## PHASE 3: Entities (10 entities)

Create all in `src/main/java/com/skilora/model/entity/`. Pure POJOs, no JavaFX imports.

### Step 3.1 — `Connection.java`
Fields: `id`, `userId1`, `userId2`, `status`, `connectionType`, `createdDate` (LocalDateTime), `lastInteraction` (LocalDateTime), `strengthScore` (int).
Transient: `otherUserName`, `otherUserPhoto`.

### Step 3.2 — `Post.java`
Fields: `id`, `authorId`, `content`, `imageUrl`, `postType`, `likesCount`, `commentsCount`, `sharesCount`, `isPublished`, `createdDate`, `updatedDate`.
Transient: `authorName`, `authorPhoto`, `isLikedByCurrentUser`.

### Step 3.3 — `PostComment.java`
Fields: `id`, `postId`, `authorId`, `content`, `createdDate`.
Transient: `authorName`, `authorPhoto`.

### Step 3.4 — `Conversation.java`
Fields: `id`, `participant1`, `participant2`, `lastMessageDate` (LocalDateTime), `isArchived1`, `isArchived2`, `createdDate`.
Transient: `otherUserName`, `otherUserPhoto`, `lastMessagePreview`, `unreadCount`.

### Step 3.5 — `Message.java`
Fields: `id`, `conversationId`, `senderId`, `content`, `isRead`, `createdDate`.
Transient: `senderName`.

### Step 3.6 — `Event.java`
Fields: `id`, `organizerId`, `title`, `description`, `eventType`, `location`, `isOnline`, `onlineLink`, `startDate` (LocalDateTime), `endDate` (LocalDateTime), `maxAttendees`, `currentAttendees`, `imageUrl`, `status`, `createdDate`.
Transient: `organizerName`, `isAttending` (boolean).

### Step 3.7 — `EventRsvp.java`
Fields: `id`, `eventId`, `userId`, `status` (GOING/MAYBE/NOT_GOING), `rsvpDate`.

### Step 3.8 — `Mentorship.java`
Fields: `id`, `mentorId`, `menteeId`, `status`, `topic`, `goals`, `startDate` (LocalDate), `endDate` (LocalDate), `rating`, `feedback`, `createdDate`.
Transient: `mentorName`, `menteeName`, `mentorPhoto`.

### Step 3.9 — `BlogArticle.java`
Fields: `id`, `authorId`, `title`, `content`, `summary`, `coverImageUrl`, `category`, `tags`, `viewsCount`, `likesCount`, `isPublished`, `publishedDate` (LocalDateTime), `createdDate`, `updatedDate`.
Transient: `authorName`, `authorPhoto`.

### Step 3.10 — `CommunityGroup.java`
Fields: `id`, `name`, `description`, `category`, `coverImageUrl`, `creatorId`, `memberCount`, `isPublic`, `createdDate`.
Transient: `creatorName`, `isMember` (boolean).

### Step 3.11 — `GroupMember.java`
Fields: `id`, `groupId`, `userId`, `role` (ADMIN/MODERATOR/MEMBER), `joinedDate`.
Transient: `userName`, `userPhoto`.

### Step 3.12 — `Achievement.java`
Fields: `id`, `userId`, `badgeType`, `title`, `description`, `iconUrl`, `earnedDate` (LocalDateTime), `rarity`, `points`.

---

## PHASE 4: Services (Priority Order)

### Step 4.1 — `ConnectionService.java`

- `int sendRequest(int fromUserId, int toUserId)` — INSERT with status=PENDING (ensure user1 < user2 for consistency)
- `boolean acceptRequest(int connectionId)` — UPDATE status=ACCEPTED
- `boolean rejectRequest(int connectionId)` — UPDATE status=REJECTED
- `boolean removeConnection(int connectionId)` — DELETE
- `List<Connection> getConnections(int userId)` — WHERE (user_id_1=? OR user_id_2=?) AND status=ACCEPTED
- `List<Connection> getPendingRequests(int userId)` — WHERE user_id_2=? AND status=PENDING
- `boolean areConnected(int userId1, int userId2)` — check if ACCEPTED connection exists
- `int getConnectionCount(int userId)` — COUNT
- `List<Connection> getSuggestions(int userId, int limit)` — users NOT yet connected, ORDER BY RAND() LIMIT ?

### Step 4.2 — `PostService.java`

- `int create(Post post)` — INSERT
- `Post findById(int id)` — SELECT with JOIN for author info
- `List<Post> getFeed(int userId, int page, int pageSize)` — posts from connections + own posts, ORDER BY created_date DESC, with LIMIT/OFFSET
- `List<Post> getByAuthor(int authorId)` — user's posts
- `boolean update(Post post)` — UPDATE
- `boolean delete(int id)` — DELETE
- `boolean toggleLike(int postId, int userId)` — INSERT or DELETE from post_likes, update likes_count
- `boolean isLikedBy(int postId, int userId)` — SELECT 1
- `int addComment(PostComment comment)` — INSERT, update comments_count
- `List<PostComment> getComments(int postId)` — with author info
- `boolean deleteComment(int commentId)` — DELETE, update count

### Step 4.3 — `MessagingService.java`

- `int getOrCreateConversation(int userId1, int userId2)` — find existing or INSERT new
- `List<Conversation> getConversations(int userId)` — with last message preview, unread count, other user info
- `int sendMessage(int conversationId, int senderId, String content)` — INSERT + update conversation lastMessageDate
- `List<Message> getMessages(int conversationId, int page, int pageSize)` — ORDER BY created_date ASC, LIMIT/OFFSET
- `boolean markAsRead(int conversationId, int userId)` — UPDATE is_read=TRUE WHERE sender_id != ? AND conversation_id = ?
- `int getUnreadCount(int userId)` — COUNT total unread messages
- `boolean archiveConversation(int conversationId, int userId)` — UPDATE is_archived_N=TRUE
- `boolean deleteConversation(int conversationId)` — DELETE

### Step 4.4 — `EventService.java`

- `int create(Event event)` — INSERT
- `Event findById(int id)` — with organizer info
- `List<Event> findUpcoming()` — WHERE start_date > NOW() AND status=UPCOMING, ORDER BY start_date
- `List<Event> findByOrganizer(int organizerId)`
- `List<Event> findByAttendee(int userId)` — JOIN event_rsvps
- `boolean update(Event event)` — UPDATE
- `boolean cancel(int id)` — UPDATE status=CANCELLED
- `boolean delete(int id)` — DELETE
- `boolean rsvp(int eventId, int userId, String status)` — INSERT/UPDATE event_rsvps, update current_attendees
- `boolean cancelRsvp(int eventId, int userId)` — DELETE from event_rsvps, update count
- `List<EventRsvp> getAttendees(int eventId)` — with user info
- `boolean isAttending(int eventId, int userId)` — check

### Step 4.5 — `MentorshipService.java`

- `int requestMentorship(int menteeId, int mentorId, String topic, String goals)` — INSERT
- `boolean accept(int id)` — UPDATE status=ACTIVE, startDate=NOW()
- `boolean complete(int id, int rating, String feedback)` — UPDATE status=COMPLETED
- `boolean cancel(int id)` — UPDATE status=CANCELLED
- `List<Mentorship> findByMentor(int mentorId)` — active mentorships
- `List<Mentorship> findByMentee(int menteeId)` — my mentors
- `List<Mentorship> findPendingForMentor(int mentorId)` — requests received
- `boolean delete(int id)` — DELETE

### Step 4.6 — `BlogService.java`

- `int create(BlogArticle article)` — INSERT
- `BlogArticle findById(int id)` — with author info + increment view count
- `List<BlogArticle> findPublished(int page, int pageSize)` — published articles
- `List<BlogArticle> findByAuthor(int authorId)` — including drafts
- `List<BlogArticle> findByCategory(String category)`
- `List<BlogArticle> search(String query)` — LIKE on title, content, tags
- `boolean update(BlogArticle article)` — UPDATE
- `boolean publish(int id)` — UPDATE is_published=TRUE, published_date=NOW()
- `boolean delete(int id)` — DELETE

### Step 4.7 — `GroupService.java`

- `int create(CommunityGroup group)` — INSERT + add creator as ADMIN member
- `CommunityGroup findById(int id)` — with creator info
- `List<CommunityGroup> findAll()` — public groups
- `List<CommunityGroup> findByMember(int userId)` — groups user belongs to
- `List<CommunityGroup> search(String query)` — LIKE on name, description
- `boolean update(CommunityGroup group)` — UPDATE
- `boolean delete(int id)` — DELETE (only creator)
- `boolean join(int groupId, int userId)` — INSERT group_members, increment count
- `boolean leave(int groupId, int userId)` — DELETE group_members, decrement count
- `boolean isMember(int groupId, int userId)` — check
- `List<GroupMember> getMembers(int groupId)` — with user info

### Step 4.8 — `AchievementService.java`

- `int award(Achievement achievement)` — INSERT
- `List<Achievement> findByUserId(int userId)` — ORDER BY earned_date DESC
- `int getTotalPoints(int userId)` — SUM(points)
- `boolean hasAchievement(int userId, String badgeType)` — check
- `void checkAndAward(int userId)` — business logic to auto-award badges:
  - "FIRST_CONNECTION" — when connection count reaches 1
  - "NETWORKER" — when connection count reaches 10
  - "SUPER_NETWORKER" — when connections reach 50
  - "FIRST_POST" — when user creates first post
  - "BLOGGER" — when user publishes first blog article
  - "MENTOR" — when user completes first mentorship
  - "EVENT_ORGANIZER" — when user creates first event
  - "COMMUNITY_BUILDER" — when user creates a group

---

## PHASE 5: Controllers & Views

### Step 5.1 — Create `CommunityController.java`

Main community hub controller with tabs:
- **Feed tab:** Social feed showing posts from connections
  - Create post (text + optional image)
  - Like/comment on posts
  - Share posts
- **Connections tab:**
  - My connections list with search
  - Pending requests (accept/reject)
  - Suggested connections
  - Send connection request
- **Messages tab:**
  - Conversation list with previews
  - Open conversation → chat view
  - Send messages
  - Unread badge count
- **Events tab:**
  - Upcoming events list
  - Create event form
  - RSVP to events
  - My events (organizing / attending)
- **Groups tab:**
  - Browse groups
  - My groups
  - Create group
  - Join/leave groups

### Step 5.2 — Create `CommunityView.fxml`

```xml
<VBox spacing="24" style="-fx-background-color: transparent;">
    <padding><Insets top="32" right="32" bottom="32" left="32"/></padding>

    <HBox spacing="16" alignment="CENTER_LEFT">
        <VBox spacing="4">
            <Label fx:id="titleLabel" text="Communauté" styleClass="h2"/>
            <Label fx:id="subtitleLabel" styleClass="text-muted"/>
        </VBox>
        <Region HBox.hgrow="ALWAYS"/>
        <TLButton fx:id="newPostBtn" text="Publier" variant="PRIMARY" onAction="#handleNewPost"/>
    </HBox>

    <HBox fx:id="tabBox" spacing="8" alignment="CENTER_LEFT"/>
    <VBox fx:id="contentPane" VBox.vgrow="ALWAYS"/>
</VBox>
```

### Step 5.3 — Create `BlogController.java` + `BlogView.fxml`

Blog section:
- List published articles as cards
- Write new article (rich text editor or textarea)
- Read article detail view
- My articles (with draft/published status)
- Category filter, search

### Step 5.4 — Create `MentorshipController.java` + `MentorshipView.fxml`

Mentorship section:
- Browse available mentors
- Request mentorship
- My mentorships (as mentor/mentee)
- Accept/reject requests
- Complete and rate mentorship

---

## PHASE 6: Navigation Integration

### Step 6.1 — Add to `MainView.java`

1. Add sidebar nav button: `createNavButton(I18n.get("nav.community"), SVG_COMMUNITY_ICON, this::showCommunityView)`
2. Implement `showCommunityView()` following standard pattern
3. Add cached view field
4. Optionally add "Blog" and "Mentorship" as separate nav items or accessible from within the community view

SVG icon for community:
```
M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8M23 21v-2a4 4 0 0 1-3-3.87M16 3.13a4 4 0 0 1 0 7.75
```

---

## PHASE 7: I18n Keys

### Step 7.1 — Add to all 4 resource bundles

**French (messages.properties / messages_fr.properties):**
```properties
# Community Module
nav.community=Communauté
community.title=Communauté
community.subtitle=Connectez-vous et partagez

# Tabs
community.tab.feed=Fil d'actualité
community.tab.connections=Connexions
community.tab.messages=Messages
community.tab.events=Événements
community.tab.groups=Groupes

# Posts
post.new=Nouvelle publication
post.placeholder=Quoi de neuf ?
post.publish=Publier
post.like=J'aime
post.comment=Commenter
post.share=Partager
post.delete=Supprimer
post.edit=Modifier
post.comments={0} commentaires
post.likes={0} j'aime
post.empty=Aucune publication

# Connections
connection.title=Connexions
connection.count={0} connexions
connection.send_request=Se connecter
connection.accept=Accepter
connection.reject=Refuser
connection.remove=Supprimer la connexion
connection.pending=Demandes en attente
connection.suggestions=Suggestions
connection.empty=Aucune connexion
connection.success.sent=Demande envoyée
connection.success.accepted=Connexion acceptée
connection.confirm.remove=Supprimer cette connexion ?

# Messages
message.title=Messages
message.new=Nouveau message
message.placeholder=Tapez votre message...
message.send=Envoyer
message.unread={0} non lu(s)
message.empty=Aucun message
message.archive=Archiver

# Events
event.title=Événements
event.new=Nouvel événement
event.type.meetup=Meetup
event.type.webinar=Webinaire
event.type.workshop=Atelier
event.type.conference=Conférence
event.type.networking=Networking
event.date=Date
event.location=Lieu
event.online=En ligne
event.attendees={0} participants
event.rsvp.going=Je participe
event.rsvp.maybe=Peut-être
event.rsvp.cancel=Annuler ma participation
event.empty=Aucun événement à venir
event.success.created=Événement créé
event.success.rsvp=Participation confirmée

# Groups
group.title=Groupes
group.new=Nouveau groupe
group.join=Rejoindre
group.leave=Quitter
group.members={0} membres
group.empty=Aucun groupe
group.success.joined=Vous avez rejoint le groupe
group.success.left=Vous avez quitté le groupe
group.success.created=Groupe créé

# Mentorship
mentorship.title=Mentorat
mentorship.request=Demander un mentor
mentorship.accept=Accepter
mentorship.complete=Terminer
mentorship.cancel=Annuler
mentorship.topic=Sujet
mentorship.goals=Objectifs
mentorship.rating=Note
mentorship.feedback=Feedback
mentorship.empty=Aucun mentorat
mentorship.as_mentor=En tant que mentor
mentorship.as_mentee=En tant que mentoré
mentorship.success.requested=Demande envoyée
mentorship.success.accepted=Mentorat accepté
mentorship.success.completed=Mentorat terminé

# Blog
blog.title=Articles
blog.new=Nouvel article
blog.publish=Publier
blog.draft=Brouillon
blog.read_more=Lire la suite
blog.views={0} vues
blog.empty=Aucun article
blog.success.published=Article publié
blog.success.created=Article créé

# Achievements
achievement.title=Badges & Récompenses
achievement.points={0} points
achievement.earned=Gagné le {0}
achievement.total_points=Points totaux: {0}
achievement.empty=Aucun badge gagné
```

**English (messages_en.properties):**
```properties
nav.community=Community
community.title=Community
community.subtitle=Connect and share

community.tab.feed=Feed
community.tab.connections=Connections
community.tab.messages=Messages
community.tab.events=Events
community.tab.groups=Groups

post.new=New Post
post.placeholder=What's on your mind?
post.publish=Post
post.like=Like
post.comment=Comment
post.share=Share
post.delete=Delete
post.edit=Edit
post.comments={0} comments
post.likes={0} likes
post.empty=No posts yet

connection.title=Connections
connection.count={0} connections
connection.send_request=Connect
connection.accept=Accept
connection.reject=Decline
connection.remove=Remove Connection
connection.pending=Pending Requests
connection.suggestions=Suggestions
connection.empty=No connections
connection.success.sent=Request sent
connection.success.accepted=Connection accepted
connection.confirm.remove=Remove this connection?

message.title=Messages
message.new=New Message
message.placeholder=Type your message...
message.send=Send
message.unread={0} unread
message.empty=No messages
message.archive=Archive

event.title=Events
event.new=New Event
event.type.meetup=Meetup
event.type.webinar=Webinar
event.type.workshop=Workshop
event.type.conference=Conference
event.type.networking=Networking
event.date=Date
event.location=Location
event.online=Online
event.attendees={0} attendees
event.rsvp.going=I'm going
event.rsvp.maybe=Maybe
event.rsvp.cancel=Cancel RSVP
event.empty=No upcoming events
event.success.created=Event created
event.success.rsvp=RSVP confirmed

group.title=Groups
group.new=New Group
group.join=Join
group.leave=Leave
group.members={0} members
group.empty=No groups
group.success.joined=You joined the group
group.success.left=You left the group
group.success.created=Group created

mentorship.title=Mentorship
mentorship.request=Request Mentor
mentorship.accept=Accept
mentorship.complete=Complete
mentorship.cancel=Cancel
mentorship.topic=Topic
mentorship.goals=Goals
mentorship.rating=Rating
mentorship.feedback=Feedback
mentorship.empty=No mentorships
mentorship.as_mentor=As Mentor
mentorship.as_mentee=As Mentee
mentorship.success.requested=Request sent
mentorship.success.accepted=Mentorship accepted
mentorship.success.completed=Mentorship completed

blog.title=Articles
blog.new=New Article
blog.publish=Publish
blog.draft=Draft
blog.read_more=Read More
blog.views={0} views
blog.empty=No articles
blog.success.published=Article published
blog.success.created=Article created

achievement.title=Badges & Rewards
achievement.points={0} points
achievement.earned=Earned on {0}
achievement.total_points=Total points: {0}
achievement.empty=No badges earned
```

**Arabic (messages_ar.properties):**
```properties
nav.community=المجتمع
community.title=المجتمع
community.subtitle=تواصل وشارك

community.tab.feed=الأخبار
community.tab.connections=الاتصالات
community.tab.messages=الرسائل
community.tab.events=الأحداث
community.tab.groups=المجموعات

post.new=منشور جديد
post.placeholder=ما الجديد؟
post.publish=نشر
post.like=إعجاب
post.comment=تعليق
post.share=مشاركة
post.delete=حذف
post.edit=تعديل
post.comments={0} تعليقات
post.likes={0} إعجابات
post.empty=لا توجد منشورات

connection.title=الاتصالات
connection.count={0} اتصالات
connection.send_request=تواصل
connection.accept=قبول
connection.reject=رفض
connection.remove=إزالة الاتصال
connection.pending=طلبات معلّقة
connection.suggestions=اقتراحات
connection.empty=لا توجد اتصالات
connection.success.sent=تم إرسال الطلب
connection.success.accepted=تم قبول الاتصال
connection.confirm.remove=إزالة هذا الاتصال؟

message.title=الرسائل
message.new=رسالة جديدة
message.placeholder=اكتب رسالتك...
message.send=إرسال
message.unread={0} غير مقروءة
message.empty=لا توجد رسائل
message.archive=أرشفة

event.title=الأحداث
event.new=حدث جديد
event.type.meetup=لقاء
event.type.webinar=ندوة عبر الإنترنت
event.type.workshop=ورشة عمل
event.type.conference=مؤتمر
event.type.networking=تواصل مهني
event.date=التاريخ
event.location=المكان
event.online=عبر الإنترنت
event.attendees={0} مشاركين
event.rsvp.going=سأحضر
event.rsvp.maybe=ربما
event.rsvp.cancel=إلغاء المشاركة
event.empty=لا توجد أحداث قادمة
event.success.created=تم إنشاء الحدث
event.success.rsvp=تم تأكيد المشاركة

group.title=المجموعات
group.new=مجموعة جديدة
group.join=انضمام
group.leave=مغادرة
group.members={0} أعضاء
group.empty=لا توجد مجموعات
group.success.joined=انضممت إلى المجموعة
group.success.left=غادرت المجموعة
group.success.created=تم إنشاء المجموعة

mentorship.title=الإرشاد
mentorship.request=طلب مرشد
mentorship.accept=قبول
mentorship.complete=إتمام
mentorship.cancel=إلغاء
mentorship.topic=الموضوع
mentorship.goals=الأهداف
mentorship.rating=التقييم
mentorship.feedback=ملاحظات
mentorship.empty=لا يوجد إرشاد
mentorship.as_mentor=كمرشد
mentorship.as_mentee=كمتدرب
mentorship.success.requested=تم إرسال الطلب
mentorship.success.accepted=تم قبول الإرشاد
mentorship.success.completed=تم إتمام الإرشاد

blog.title=المقالات
blog.new=مقال جديد
blog.publish=نشر
blog.draft=مسودة
blog.read_more=اقرأ المزيد
blog.views={0} مشاهدات
blog.empty=لا توجد مقالات
blog.success.published=تم نشر المقال
blog.success.created=تم إنشاء المقال

achievement.title=الشارات والمكافآت
achievement.points={0} نقاط
achievement.earned=حصل عليها في {0}
achievement.total_points=مجموع النقاط: {0}
achievement.empty=لم يتم الحصول على شارات
```

---

## PHASE 8: Verification

### Step 8.1 — Compile: `mvn compile -q`
### Step 8.2 — Run & Test:
1. Community nav button appears
2. Feed loads and shows posts
3. Creating/liking/commenting on posts works
4. Connections: send/accept/reject/remove
5. Messages: send/receive, conversation list
6. Events: create, RSVP, view
7. Groups: create, join, leave
8. Achievements auto-award on milestones

---

## RULES

1. **NO DAO classes.** Services contain JDBC code directly.
2. **NO JavaFX imports in entity or service classes.**
3. **All UI text via `I18n.get("key")`.**
4. **Singleton pattern** on all services.
5. **Async DB calls** via `javafx.concurrent.Task`.
6. **Follow existing code patterns exactly.**
7. **Table creation in `DatabaseInitializer.java`.**
8. **Use TL* components** for UI.
9. **Handle null safely.**
10. **Do NOT modify FeedController or FeedView** — those are for the job feed, not social feed.
11. **Do NOT break existing functionality.** Only add new files and modify DatabaseInitializer + MainView.
