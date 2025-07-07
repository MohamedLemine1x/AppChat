# 🔧 Setup Guide for VibeApp

This guide will help you set up VibeApp on your local machine. Follow these steps carefully to ensure proper configuration.

## 📋 Prerequisites Checklist

- [ ] Java 8 or higher installed
- [ ] Git installed
- [ ] Firebase account (free tier is sufficient)
- [ ] Internet connection for Firebase setup

## 🛠️ Step-by-Step Setup

### 1. Clone the Repository

```bash
git clone https://github.com/MohamedLemine1x/AppChat.git
cd AppChat
```

### 2. Firebase Project Setup

#### 2.1 Create a Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add project"
3. Enter a project name (e.g., "my-vibeapp")
4. Continue through the setup process
5. Choose your analytics preferences

#### 2.2 Enable Firebase Realtime Database

1. In your Firebase project dashboard, click on "Realtime Database"
2. Click "Create Database"
3. Choose "Start in test mode" (for development)
4. Select your preferred location
5. Click "Done"

#### 2.3 Generate Service Account Key

1. Go to Project Settings (gear icon) > Service Accounts
2. Click "Generate New Private Key"
3. **Important**: Save this file securely - it contains sensitive credentials
4. Rename the downloaded file to `firebase-config.json`

### 3. Configure Application

#### 3.1 Create Configuration Files

```bash
# Create resources directory if it doesn't exist
mkdir -p resources

# Copy the example configuration
cp firebase-config.example.json resources/firebase-config.json
```

#### 3.2 Update Firebase Configuration

Edit `resources/firebase-config.json` with your actual Firebase credentials:

```json
{
  "type": "service_account",
  "project_id": "YOUR_PROJECT_ID",
  "private_key_id": "YOUR_PRIVATE_KEY_ID",
  "private_key": "-----BEGIN PRIVATE KEY-----\nYOUR_PRIVATE_KEY\n-----END PRIVATE KEY-----\n",
  "client_email": "firebase-adminsdk-xxxxx@YOUR_PROJECT_ID.iam.gserviceaccount.com",
  "client_id": "YOUR_CLIENT_ID",
  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
  "token_uri": "https://oauth2.googleapis.com/token",
  "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
  "client_x509_cert_url": "https://www.googleapis.com/robot/v1/metadata/x509/firebase-adminsdk-xxxxx%40YOUR_PROJECT_ID.iam.gserviceaccount.com",
  "universe_domain": "googleapis.com"
}
```

#### 3.3 Create Required Directories

```bash
# Create profile images directory
mkdir -p resources/profiles

# Create shared images directory
mkdir -p shared_images
```

### 4. Build and Run

#### 4.1 Using the Batch File (Windows)

```bash
./run.bat
```

#### 4.2 Manual Compilation and Execution

```bash
# Compile the application
javac -cp "lib/*" src/**/*.java

# Run the application
java -cp "src:lib/*" Main
```

## 🔐 Security Configuration

### Environment Variables (Recommended)

For enhanced security, you can use environment variables:

```bash
# Set Firebase configuration path
export FIREBASE_CONFIG_PATH=/path/to/your/firebase-config.json

# Set Firebase database URL
export FIREBASE_DATABASE_URL=https://your-project-id-default-rtdb.firebaseio.com

# Run with environment variables
java -Dfirebase.config.path=$FIREBASE_CONFIG_PATH -Dfirebase.database.url=$FIREBASE_DATABASE_URL -cp "src:lib/*" Main
```

### File Permissions

```bash
# Secure your configuration files
chmod 600 resources/firebase-config.json
```

## 🚧 Troubleshooting

### Common Setup Issues

#### 1. Firebase Connection Failed

**Error**: `Connection to Firebase failed`

**Solutions**:
- Verify `firebase-config.json` is in the correct location
- Check that the Firebase project ID matches your configuration
- Ensure Realtime Database is enabled in your Firebase project
- Verify service account has proper permissions

#### 2. Class Not Found

**Error**: `ClassNotFoundException` or `NoClassDefFoundError`

**Solutions**:
- Ensure all JAR files are in the `lib/` directory
- Check your classpath includes all required libraries
- Verify Java version compatibility

#### 3. Permission Denied

**Error**: `java.io.FileNotFoundException` or permission errors

**Solutions**:
- Check file permissions on configuration files
- Ensure the application has read access to the `resources/` directory
- Verify directory structure is correct

#### 4. Database Rules

If you encounter database permission errors:

1. Go to Firebase Console > Realtime Database > Rules
2. For testing, use these rules (NOT for production):

```json
{
  "rules": {
    ".read": true,
    ".write": true
  }
}
```

## 📝 Configuration Files Summary

| File | Purpose | Required | Location |
|------|---------|----------|----------|
| `firebase-config.json` | Firebase credentials | Yes | `resources/` |
| `user_profiles.properties` | User profile mappings | Auto-generated | `resources/` |
| `remember_me.properties` | Session persistence | Auto-generated | Root |

## 🏃‍♂️ Quick Start Checklist

- [ ] Clone repository
- [ ] Create Firebase project
- [ ] Enable Realtime Database
- [ ] Generate service account key
- [ ] Copy key to `resources/firebase-config.json`
- [ ] Create required directories
- [ ] Run the application
- [ ] Test login/registration

## 📞 Need Help?

If you encounter issues during setup:

1. Check the troubleshooting section above
2. Verify all configuration files are in place
3. Ensure Firebase project is properly configured
4. Create an issue on GitHub with:
   - Your operating system
   - Java version
   - Error messages (without sensitive information)
   - Steps you've already tried

## 🎯 Next Steps

After successful setup:

1. Create a test user account
2. Upload a profile picture
3. Start chatting!
4. Explore the group creation features
5. Configure user preferences

---

**Note**: Keep your `firebase-config.json` file secure and never commit it to version control!