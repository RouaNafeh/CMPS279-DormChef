# DormChef

DormChef is an Android social cooking app for everyday home cooks. It helps users discover recipe posts, generate recipe ideas from ingredients, save and like dishes, follow creators, and cook recipes step by step with timed instructions.

## Product Summary

The app combines practical cooking help with a social layer:

- create an account with Firebase Authentication
- verify email before entering the app
- reset password from the login screen
- browse a community recipe feed
- search recipe posts by title or author
- save and like posts
- comment on recipes and leave ratings
- follow creators and view public profiles
- receive in-app and push notifications for follows, likes, comments, and reviews
- create and edit recipe posts with images
- generate recipe drafts with AI from ingredients
- use Cooking Mode for step-by-step timed instructions

## Tech Stack

- Java
- Android Studio
- ViewBinding
- RecyclerView
- Material Components
- Firebase Authentication
- Cloud Firestore
- Firebase Storage
- Firebase Cloud Messaging
- Firebase AI / Gemini
- Firebase Cloud Functions

## Main Screens

- `LandingActivity`: entry screen
- `LoginActivity` / `SignupActivity`: auth flow
- `HomeActivity`: community recipe feed
- `CreatePostActivity`: recipe post creation and editing
- `PostDetailActivity`: recipe details, save/like, comments, ratings, cooking mode
- `ProfileActivity`: private profile and authored posts
- `PublicProfileActivity`: creator profile and follow flow
- `SavedPostsActivity` / `LikedPostsActivity`: personal collections
- `NotificationsActivity`: notification center
- `TimerActivity`: guided cooking flow

## Current Status

- The app builds successfully with Gradle.
- The main product flow is Firebase-backed and centered on social recipe posts.
- Email verification, password reset, push token sync, account deletion, and post cleanup are implemented.
- Legacy local SQLite recipe browsing has been retired from the app.

## Run The App

1. Open the project in Android Studio.
2. Let Gradle sync finish.
3. Configure Firebase credentials as required by the project.
4. Run the app on an emulator or Android device.

The launcher screen starts from `LandingActivity`.

## Repository Notes

- The active product is the social cooking app, not the older local recipe browser.
- Some copy still uses the word "recipe" broadly for posts and cooking content.
- Automated test coverage is still minimal and should be expanded in a future pass.
