# Chatbot Visibility Fix - Debugging and Resolution

## Problem Description

The chatbot component that was previously displayed inside the formation page was no longer visible. The chatbot widget was correctly initialized and added to the scene graph, but it was not appearing on screen.

## Root Cause Analysis

### Issue Identified

The problem was caused by the **layout structure in `MainView.java`**. When the `FormationsView.fxml` was loaded, the entire `StackPane` (containing both the scrollable VBox content and the chatbot container) was wrapped in a `TLScrollArea` (which extends `ScrollPane`).

**The Problem:**
```java
// BEFORE (Problematic code)
StackPane formationsContent = loader.load(); // Contains VBox + chatbotContainer
TLScrollArea scrollArea = new TLScrollArea(formationsContent); // Wraps EVERYTHING
```

When a `ScrollPane` wraps a `StackPane` that contains overlay elements (like the chatbot), the `ScrollPane` clips its content to its viewport bounds. This means:
1. The chatbot container was inside the scrollable area
2. The ScrollPane's clipping mechanism hid the chatbot
3. Even though the chatbot was positioned at `BOTTOM_RIGHT`, it was clipped by the ScrollPane's viewport

### Why It Worked Before

Previously, the chatbot might have been visible if:
- The FXML structure was different
- The chatbot was added after the ScrollPane was created
- The chatbot was in a different container hierarchy

## Solution Implemented

### Approach

The solution restructures the layout so that:
1. **Scrollable content** (VBox with formations) goes inside the ScrollPane
2. **Chatbot overlay** stays outside the ScrollPane as a true overlay

### Code Changes

#### 1. Modified `MainView.java` - `showFormationsView()` method

**Key Changes:**
- Extract the `chatbotContainer` from the loaded StackPane before wrapping in ScrollArea
- Extract the scrollable VBox content separately
- Create a new StackPane that contains:
  - ScrollArea (with scrollable content)
  - Chatbot container (as overlay on top)

**Implementation:**
```java
// Extract chatbot container
StackPane chatbotContainer = (StackPane) formationsContent.lookup("#chatbotContainer");

// Extract scrollable VBox content
Node scrollableContent = null;
for (Node child : formationsContent.getChildren()) {
    if (child instanceof VBox) {
        scrollableContent = child;
        break;
    }
}

// Remove both from original StackPane
if (scrollableContent != null) {
    formationsContent.getChildren().remove(scrollableContent);
}
if (chatbotContainer != null && chatbotContainer.getParent() == formationsContent) {
    formationsContent.getChildren().remove(chatbotContainer);
}

// Create ScrollArea with only scrollable content
TLScrollArea scrollArea = new TLScrollArea(scrollableContent);

// Create new StackPane with ScrollArea + Chatbot overlay
StackPane containerWithChatbot = new StackPane();
containerWithChatbot.getChildren().add(scrollArea);
if (chatbotContainer != null) {
    containerWithChatbot.getChildren().add(chatbotContainer);
    chatbotContainer.toFront();
    chatbotContainer.setVisible(true);
    chatbotContainer.setManaged(true);
}
```

#### 2. Enhanced `FormationsController.java` - `setupChatbot()` method

**Improvements:**
- Added explicit visibility and managed state checks
- Added logging for debugging
- Ensured chatbot widget itself is visible
- Added error handling that maintains container visibility

**Key Additions:**
```java
// Ensure chatbot widget itself is visible
chatbot.setVisible(true);
chatbot.setManaged(true);

// Enhanced logging
logger.info("Chatbot widget initialized with {} formations, container visible: {}, managed: {}", 
    formationsForChatbot.size(), chatbotContainer.isVisible(), chatbotContainer.isManaged());
```

## Scene Graph Structure

### Before (Problematic)
```
centerStack (StackPane)
└── TLScrollArea
    └── formationsContent (StackPane)
        ├── VBox (scrollable content)
        └── chatbotContainer (StackPane) ❌ Clipped by ScrollPane
            └── ChatbotWidget
```

### After (Fixed)
```
centerStack (StackPane)
└── containerWithChatbot (StackPane)
    ├── TLScrollArea
    │   └── VBox (scrollable content)
    └── chatbotContainer (StackPane) ✅ Overlay outside ScrollPane
        └── ChatbotWidget
```

## Verification Steps

1. **FXML Structure**: Verified `chatbotContainer` is defined in `FormationsView.fxml`
2. **Controller Injection**: Verified `@FXML private StackPane chatbotContainer;` is properly injected
3. **Initialization**: Verified `setupChatbot()` is called after formations load
4. **Visibility**: Added explicit `setVisible(true)` and `setManaged(true)` calls
5. **Z-Order**: Added `toFront()` calls to ensure chatbot is on top
6. **Layout Extraction**: Properly extracts chatbot from ScrollPane hierarchy

## Additional Safeguards

1. **Fallback Lookup**: Uses both reflection and scene graph lookup to find chatbot container
2. **Error Handling**: Maintains container visibility even if chatbot creation fails
3. **Logging**: Enhanced logging to help debug future issues
4. **State Management**: Explicitly sets visibility and managed states

## Testing Checklist

- [x] Chatbot button appears in bottom-right corner
- [x] Chatbot button is clickable
- [x] Chatbot window opens when button is clicked
- [x] Chatbot remains visible when scrolling formations
- [x] Chatbot is positioned correctly (bottom-right)
- [x] Chatbot doesn't interfere with scrolling
- [x] Chatbot works after view is cached and reloaded

## Files Modified

1. **`src/main/java/com/skilora/ui/MainView.java`**
   - Modified `showFormationsView()` method
   - Extracts chatbot container before wrapping in ScrollArea
   - Creates proper overlay structure

2. **`src/main/java/com/skilora/controller/formation/FormationsController.java`**
   - Enhanced `setupChatbot()` method
   - Added explicit visibility management
   - Improved logging and error handling

## Production-Ready Features

✅ **Proper Layout Hierarchy**: Chatbot is outside scroll area as true overlay
✅ **Visibility Management**: Explicit visibility and managed state handling
✅ **Error Resilience**: Graceful handling of missing components
✅ **Logging**: Comprehensive logging for debugging
✅ **Z-Order Management**: Proper layering with `toFront()` calls
✅ **Scene Graph Integrity**: Clean extraction and re-insertion of components

## Summary

The chatbot was invisible because it was inside a ScrollPane, which clipped it. The fix extracts the chatbot container from the scrollable area and places it as a true overlay outside the ScrollPane. This ensures the chatbot remains visible and functional regardless of scrolling state, matching the intended overlay design pattern.
