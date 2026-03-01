# Training Rating System - Architecture Documentation

## Overview

This document describes the complete Like/Dislike and 5-star rating system implementation for formations in the Skilora Tunisia e-learning platform. The system allows users to rate completed formations with a 5-star rating and optional like/dislike feedback.

## Features

- ✅ **5-Star Rating**: Interactive star rating component (1-5 stars) with hover effects
- ✅ **Like/Dislike**: Optional like/dislike buttons for additional feedback
- ✅ **One Rating Per User**: Enforced at database level (UNIQUE constraint)
- ✅ **Completion Requirement**: Only available after training completion
- ✅ **Statistics Display**: Shows aggregated statistics (average rating, total ratings, likes/dislikes)
- ✅ **Dynamic Updates**: UI updates immediately after submission without page refresh
- ✅ **Professional UI**: Clean, modern JavaFX interface with smooth interactions

## Architecture

### 1. Database Layer

#### Table: `training_ratings`

```sql
CREATE TABLE training_ratings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    training_id INT NOT NULL,
    is_liked BOOLEAN DEFAULT NULL,  -- TRUE = liked, FALSE = disliked, NULL = no preference
    star_rating INT NOT NULL CHECK (star_rating >= 1 AND star_rating <= 5),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_user_training_rating (user_id, training_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (training_id) REFERENCES trainings(id) ON DELETE CASCADE
);
```

**Key Constraints:**
- `UNIQUE(user_id, training_id)`: Ensures one rating per user per training
- `CHECK (star_rating >= 1 AND star_rating <= 5)`: Validates star rating range
- Foreign keys with CASCADE: Automatically deletes ratings when user or training is deleted

**Indexes:**
- `idx_user_id`: Fast lookup by user
- `idx_training_id`: Fast lookup by training
- `idx_star_rating`: Fast aggregation queries
- `idx_is_liked`: Fast like/dislike statistics

### 2. Entity Layer

#### `TrainingRating.java`
- **Location**: `src/main/java/com/skilora/model/entity/formation/TrainingRating.java`
- **Purpose**: Represents a user's rating for a training
- **Fields**:
  - `id`: Primary key
  - `userId`: Reference to user
  - `trainingId`: Reference to training
  - `isLiked`: Boolean (TRUE = like, FALSE = dislike, NULL = no preference)
  - `starRating`: Integer (1-5)
  - `createdAt`, `updatedAt`: Timestamps

### 3. Repository Layer

#### `TrainingRatingRepository.java` (Interface)
- **Location**: `src/main/java/com/skilora/model/repository/TrainingRatingRepository.java`
- **Methods**:
  - `save(TrainingRating)`: Save or update rating
  - `findByUserIdAndTrainingId(int, int)`: Get user's rating for a training
  - `existsByUserIdAndTrainingId(int, int)`: Check if rating exists
  - `delete(int)`: Delete rating
  - `getStatistics(int)`: Get aggregated statistics for a training

#### `TrainingRatingRepositoryImpl.java` (Implementation)
- **Location**: `src/main/java/com/skilora/model/repository/impl/TrainingRatingRepositoryImpl.java`
- **Database Operations**: All SQL queries and result mapping
- **Statistics Query**: Aggregates total ratings, likes, dislikes, and average rating

### 4. Service Layer

#### `TrainingRatingService.java`
- **Location**: `src/main/java/com/skilora/service/formation/TrainingRatingService.java`
- **Business Logic**:
  - **Validation**: Ensures training is completed before rating
  - **Duplicate Prevention**: Checks if user already rated (enforced at DB level)
  - **Rating Submission**: Handles both new ratings and updates
  - **Statistics**: Retrieves aggregated statistics
  - **Permission Checks**: `canUserRate()`, `canUserUpdateRating()`

**Key Methods:**
```java
submitRating(userId, trainingId, isLiked, starRating)
getUserRating(userId, trainingId)
hasUserRated(userId, trainingId)
getStatistics(trainingId)
canUserRate(userId, trainingId)
canUserUpdateRating(userId, trainingId)
```

**Added Method to `TrainingEnrollmentService`:**
- `isTrainingCompleted(userId, trainingId)`: Checks if training is completed

### 5. UI Components

#### `StarRatingComponent.java`
- **Location**: `src/main/java/com/skilora/framework/components/StarRatingComponent.java`
- **Purpose**: Interactive 5-star rating component
- **Features**:
  - 5 clickable stars with hover effects
  - Visual feedback (gold when filled, gray when empty)
  - Scale animation on hover
  - Can be set to read-only mode
  - JavaFX `Region`-based implementation

**Usage:**
```java
StarRatingComponent stars = new StarRatingComponent(true); // interactive
stars.setRating(4); // Set to 4 stars
int rating = stars.getRating(); // Get current rating
```

#### `TrainingRatingPanel.java`
- **Location**: `src/main/java/com/skilora/framework/components/TrainingRatingPanel.java`
- **Purpose**: Complete rating panel with all controls
- **Components**:
  - Star rating component
  - Like/Dislike buttons
  - Submit button
  - Statistics display
  - Message feedback area

**Features:**
- Auto-loads existing rating if user already rated
- Disables controls after submission
- Shows success/error messages
- Updates statistics dynamically
- Professional styling with gradients and hover effects

### 6. Controller Integration

#### `TrainingDetailController.java`
- **Location**: `src/main/java/com/skilora/controller/formation/TrainingDetailController.java`
- **Integration**:
  - Added `ratingContainer` FXML field
  - Added `showRatingPanelIfCompleted()` method
  - Panel is shown only when:
    - User is logged in
    - User is enrolled
    - Training is completed

**FXML Changes:**
- Added `<VBox fx:id="ratingContainer">` in `TrainingDetailView.fxml`
- Positioned after action buttons, before content section

## Data Flow

### Rating Submission Flow

1. **User completes training** → `enrollment.completed = true`
2. **User opens training detail view** → `TrainingDetailController` checks completion
3. **Rating panel appears** → `TrainingRatingPanel` is instantiated
4. **User selects stars and like/dislike** → UI updates in real-time
5. **User clicks "Submit"** → `TrainingRatingService.submitRating()` is called
6. **Service validates**:
   - Training is completed ✓
   - Star rating is 1-5 ✓
   - User hasn't already rated (or allows update) ✓
7. **Repository saves** → `TrainingRatingRepository.save()`
8. **UI updates**:
   - Controls disabled
   - Statistics refreshed
   - Success message shown

### Statistics Retrieval Flow

1. **Panel loads** → `TrainingRatingPanel.loadStatistics()`
2. **Service calls** → `TrainingRatingService.getStatistics(trainingId)`
3. **Repository queries** → Aggregates all ratings for training
4. **Statistics displayed** → Average rating, total ratings, likes/dislikes

## Security & Validation

### Backend Validation
- ✅ Training completion check (prevents rating incomplete trainings)
- ✅ Star rating range validation (1-5)
- ✅ Database UNIQUE constraint (prevents duplicate ratings)
- ✅ Foreign key constraints (data integrity)

### Frontend Validation
- ✅ Submit button disabled until star rating selected
- ✅ Visual feedback for invalid inputs
- ✅ Error messages for failed submissions
- ✅ Read-only mode after submission

## Files Created/Modified

### New Files
1. `create_training_ratings_table.sql` - SQL migration script
2. `src/main/java/com/skilora/model/entity/formation/TrainingRating.java` - Entity
3. `src/main/java/com/skilora/model/repository/TrainingRatingRepository.java` - Repository interface
4. `src/main/java/com/skilora/model/repository/impl/TrainingRatingRepositoryImpl.java` - Repository implementation
5. `src/main/java/com/skilora/service/formation/TrainingRatingService.java` - Business logic
6. `src/main/java/com/skilora/framework/components/StarRatingComponent.java` - Star UI component
7. `src/main/java/com/skilora/framework/components/TrainingRatingPanel.java` - Rating panel
8. `TRAINING_RATING_SYSTEM_ARCHITECTURE.md` - This documentation

### Modified Files
1. `src/main/java/com/skilora/config/DatabaseInitializer.java` - Added table creation
2. `src/main/java/com/skilora/service/formation/TrainingEnrollmentService.java` - Added `isTrainingCompleted()` method
3. `src/main/java/com/skilora/controller/formation/TrainingDetailController.java` - Integrated rating panel
4. `src/main/resources/com/skilora/view/TrainingDetailView.fxml` - Added rating container
5. `skilora.sql` - Added training_ratings table schema

## Usage Example

```java
// In TrainingDetailController
private void showRatingPanelIfCompleted() {
    if (currentUser == null || enrollment == null || !enrollment.isCompleted()) {
        return;
    }
    
    TrainingRatingPanel ratingPanel = new TrainingRatingPanel(
        currentUser.getId(), 
        training.getId()
    );
    
    ratingContainer.getChildren().add(ratingPanel);
}
```

## Testing Checklist

- [ ] User can rate only after completing training
- [ ] User can submit only one rating per training
- [ ] Star rating is required (1-5)
- [ ] Like/dislike is optional
- [ ] Statistics display correctly
- [ ] UI updates dynamically after submission
- [ ] Existing rating is loaded and displayed
- [ ] Controls are disabled after submission
- [ ] Error messages show for invalid inputs
- [ ] Database constraints prevent duplicates

## Future Enhancements

- [ ] Allow users to update their rating (currently one-time only)
- [ ] Add comment/review text field
- [ ] Show rating distribution chart
- [ ] Filter formations by rating
- [ ] Sort formations by average rating
- [ ] Export ratings data for analytics

## Notes

- The system uses a singleton pattern for services (thread-safe)
- All database operations use prepared statements (SQL injection prevention)
- UI updates are done on JavaFX Application Thread (thread safety)
- Statistics are calculated on-demand (no caching, always fresh)
- The rating panel is only visible for completed trainings (UX best practice)
