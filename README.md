# CashFlow
A simple and intuitive Android mobile application that helps users log daily income and expenses, edit past records, and visualize spending habits through percentage-based charts.

## Features

### Main Interface - Financial Overview
- **Top Summary Area**: Displays total income, total expenses, and balance for the current month
- **Recent Transactions List**: Shows all recent records, with swipeable scrolling, realtime color-coded amounts, and optional notes
- **Central Add Button**: A large "+" button centered at the bottom that opens a dialog with enlarged "Record Income" / "Record Expense" choices

### Record Transaction Interface
- **Type Selection**: Toggle between Income/Expense
- **Amount Input**: Improved decimal-friendly numeric keyboard with comma normalization
- **Category Selection**: Dropdown includes Rent alongside other expense types, plus income-specific options
- **Date Selection**: Date picker, defaults to today
- **Note Input**: Optional multi-line text field
- **Save Button**: Saves the record to SQLite, supports editing existing entries after tapping a list item

### Statistics Interface
- **Pie Chart Only Colors**: Pie slices use colors only while a vertical legend on the lower-left shows category names plus percentages
- **Percentage View**: Slices express percentage of total expenses for a week/month/year or "all time" range
- **Filtering**: "This Week" matches Monday–Sunday, "This Month" spans the full calendar month, and year/all-time choices refresh immediately

## Technical Requirements Met

✅ **Android API**: Uses Android SDK 24-34  
✅ **Graphical User Interface**: Material Design components with CardView, RecyclerView, etc.  
✅ **UI Components & 2D Graphics**: Custom layouts with charts and visual elements  
✅ **Splash Screen**: SplashActivity with 2-second display and vibration feedback  
✅ **About Screen**: Displays CashFlow version and 2025 copyright details  
✅ **Data Storage**: SQLite database using DatabaseHelper class  
✅ **Multi-threading**: Background threads for database operations using Handler and Thread  
✅ **Multimedia**: Vibration effects on splash screen

## Setup Instructions

1. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the `AccountApp` folder and select it

2. **Sync Gradle**
   - Android Studio should automatically sync Gradle dependencies
   - If not, click "Sync Now" when prompted

3. **Run the App**
   - Connect an Android device or start an emulator
   - Click the "Run" button or press Shift+F10
   - The app will install and launch automatically

## Project Structure

```
AccountApp/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/accountapp/
│   │   │   ├── model/
│   │   │   │   └── Transaction.java
│   │   │   ├── db/
│   │   │   │   └── DatabaseHelper.java
│   │   │   └── ui/
│   │   │       ├── SplashActivity.java
│   │   │       ├── MainActivity.java
│   │   │       ├── AddTransactionActivity.java
│   │   │       ├── StatisticsActivity.java
│   │   │       ├── AboutActivity.java
│   │   │       └── TransactionAdapter.java
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   ├── activity_splash.xml
│   │   │   │   ├── activity_main.xml
│   │   │   │   ├── activity_add_transaction.xml
│   │   │   │   ├── activity_statistics.xml
│   │   │   │   ├── activity_about.xml
│   │   │   │   └── item_transaction.xml
│   │   │   ├── menu/
│   │   │   │   └── main_menu.xml
│   │   │   └── values/
│   │   │       ├── strings.xml
│   │   │       ├── colors.xml
│   │   │       └── styles.xml
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
├── settings.gradle
└── gradle.properties
```

## Dependencies

- AndroidX AppCompat
- Material Design Components
- ConstraintLayout
- RecyclerView
- CardView
- MPAndroidChart (for pie charts)

## Database Schema

The app uses SQLite with a single table `transactions`:

- `id`: INTEGER PRIMARY KEY AUTOINCREMENT
- `type`: TEXT NOT NULL (Income/Expense)
- `amount`: REAL NOT NULL
- `category`: TEXT NOT NULL
- `date`: TEXT NOT NULL (format: yyyy-MM-dd)
- `note`: TEXT

## Notes

- Minimum SDK: 24 (Android 7.0)
- Target SDK: 34 (Android 14)
- The app requires vibration permission for splash screen feedback
- All database operations run in background threads to avoid blocking the UI

