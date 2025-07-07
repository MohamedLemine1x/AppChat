# VibeApp 💬

<div align="center">
  <img src="https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white" alt="Java">
  <img src="https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black" alt="Firebase">
  <img src="https://img.shields.io/badge/Swing-GUI-blue?style=for-the-badge" alt="Swing">
  <img src="https://img.shields.io/badge/License-MIT-green?style=for-the-badge" alt="License">
</div>

<div align="center">
  <h3>🚀 A modern, real-time chat application built with Java and Firebase</h3>
  <p>VibeApp delivers a seamless chatting experience with secure authentication, profile management, and beautiful UI components.</p>
</div>

---

## ✨ Key Features

### 💬 **Communication**
- **Real-time Messaging**: Instant messaging with Firebase Realtime Database
- **Group Chats**: Create and manage group conversations with multiple participants
- **Message History**: Persistent chat history with message timestamps
- **Online Status**: Real-time user presence indicators

### 🔐 **Security & Authentication**
- **Secure Login/Register**: Firebase-powered authentication system
- **Email Verification**: Secure email-based account verification
- **Password Reset**: Forgot password functionality with email recovery
- **Session Management**: Secure session handling with "Remember Me" option

### 👤 **Profile Management**
- **Custom Avatars**: Upload and manage profile pictures
- **User Settings**: Comprehensive user preferences and settings
- **Profile Customization**: Personalize your chat experience

### 🎨 **Modern UI/UX**
- **Beautiful Interface**: Modern, responsive design with custom components
- **Smooth Animations**: Fluid animations and transitions
- **Intuitive Design**: User-friendly interface with modern aesthetics
- **Responsive Layout**: Adaptive design for different screen sizes

---

## 🚀 Technology Stack

<table>
  <tr>
    <td align="center">
      <img src="https://raw.githubusercontent.com/devicons/devicon/master/icons/java/java-original.svg" alt="Java" width="60" height="60"/>
      <br><b>Java</b>
      <br>Core Development
    </td>
    <td align="center">
      <img src="https://raw.githubusercontent.com/devicons/devicon/master/icons/firebase/firebase-plain.svg" alt="Firebase" width="60" height="60"/>
      <br><b>Firebase</b>
      <br>Backend Services
    </td>
    <td align="center">
      <img src="https://raw.githubusercontent.com/devicons/devicon/master/icons/google/google-original.svg" alt="Google APIs" width="60" height="60"/>
      <br><b>Google APIs</b>
      <br>Authentication
    </td>
    <td align="center">
      <img src="https://raw.githubusercontent.com/devicons/devicon/master/icons/java/java-original.svg" alt="Java Swing" width="60" height="60"/>
      <br><b>Java Swing</b>
      <br>GUI Framework
    </td>
  </tr>
</table>

### Backend & Database
- **Firebase Realtime Database** - Real-time data synchronization
- **Firebase Admin SDK** - Server-side Firebase operations
- **Firebase Authentication** - User management and security

### Frontend & UI
- **Java Swing** - Desktop GUI framework
- **Custom Components** - Reusable UI elements
- **Modern Design** - Contemporary interface patterns

### Communication & Utilities
- **JavaMail API** - Email functionality
- **Gson** - JSON parsing and serialization
- **HTTP Client** - API communications

---

## 📋 Prerequisites

Before running this application, ensure you have:

- ☑️ **Java 8 or higher** installed
- ☑️ **Firebase project** set up
- ☑️ **Firebase Admin SDK** service account key
- ☑️ **Internet connection** for Firebase operations

---

## 🛠️ Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/MohamedLemine1x/AppChat.git
cd AppChat
```

### 2. Firebase Configuration

⚠️ **Important**: This application requires Firebase configuration that is not included in the repository for security reasons.

<details>
<summary>📖 <b>Detailed Firebase Setup Guide</b></summary>

1. **Create a Firebase Project**
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Click "Add project" and follow the setup wizard

2. **Enable Firebase Realtime Database**
   - In your Firebase project, go to "Realtime Database"
   - Click "Create Database" and choose "Start in test mode"

3. **Generate Service Account Key**
   - Go to Project Settings > Service Accounts
   - Click "Generate New Private Key"
   - Save the JSON file as `firebase-config.json`

4. **Configure the Application**
   ```bash
   cp firebase-config.example.json resources/firebase-config.json
   ```
   Then edit `resources/firebase-config.json` with your Firebase credentials

</details>

### 3. Build and Run

```bash
# Using the provided batch file (Windows)
run.bat

# Or compile and run manually
javac -cp "lib/*" src/**/*.java
java -cp "src:lib/*" Main
```

### 4. Create Your Account

1. Launch the application
2. Click "Register" to create a new account
3. Verify your email address
4. Start chatting!

---

## 🏗️ Project Architecture

```
VibeApp/
├── 📁 src/
│   ├── 📁 models/           # Data models (User, Message, Chat, etc.)
│   ├── 📁 services/         # Business logic and Firebase operations
│   │   ├── AuthService.java      # Authentication handling
│   │   ├── ChatService.java      # Chat operations
│   │   ├── FirebaseService.java  # Firebase integration
│   │   └── EmailService.java     # Email functionality
│   ├── 📁 ui/
│   │   ├── 📁 components/   # Reusable UI components
│   │   │   ├── AnimatedButton.java
│   │   │   ├── ModernTextField.java
│   │   │   └── ProfileImageEditor.java
│   │   └── 📁 pages/        # Application pages
│   │       ├── LoginPage.java
│   │       ├── MainChat.java
│   │       └── UserSettings.java
│   └── 📁 utils/           # Utility classes
├── 📁 resources/           # Configuration files (not in repo)
├── 📁 lib/                 # External libraries
├── 📁 pictures/           # UI assets and icons
└── 📄 firebase-config.example.json  # Configuration template
```

---

## 🔐 Security & Privacy

### Security Features
- 🔒 **End-to-end Authentication** via Firebase
- 🔐 **Secure Password Handling** (no plaintext storage)
- 📧 **Email Verification** required for account activation
- 🛡️ **Session Management** with secure tokens
- 🔑 **API Key Protection** (excluded from version control)

### Privacy Protection
- 🚫 **No Sensitive Data in Repository**
- 🔒 **Local Configuration** required for each installation
- 🛡️ **Secure Data Transmission** via HTTPS
- 👤 **User Data Encryption** in Firebase

---

## 📚 API Reference

### Firebase Database Structure

```json
{
  "users": {
    "userId": {
      "email": "user@example.com",
      "username": "username",
      "profileImage": "profile_url",
      "status": "online",
      "lastSeen": "timestamp",
      "preferences": {
        "theme": "light",
        "notifications": true
      }
    }
  },
  "chats": {
    "chatId": {
      "name": "Chat Name",
      "type": "private|group",
      "participants": ["userId1", "userId2"],
      "messages": {
        "messageId": {
          "senderId": "userId",
          "content": "Message content",
          "timestamp": "timestamp",
          "type": "text|image|file"
        }
      },
      "metadata": {
        "created": "timestamp",
        "lastMessage": "timestamp"
      }
    }
  }
}
```

---

## 🚧 Installation Requirements

<details>
<summary>📋 <b>Missing Configuration Files</b></summary>

The following files are required but not included in the repository for security:

1. **`resources/firebase-config.json`** - Firebase service account configuration
2. **`resources/user_profiles.properties`** - User profile mappings (auto-generated)
3. **`remember_me.properties`** - Session persistence (auto-generated)

</details>

<details>
<summary>🔧 <b>System Requirements</b></summary>

- **Operating System**: Windows 10+, macOS 10.14+, Linux (Ubuntu 18.04+)
- **Java Version**: JDK 8 or higher
- **Memory**: Minimum 512MB RAM
- **Storage**: 100MB free space
- **Network**: Internet connection required

</details>

---

## 🤝 Contributing

We welcome contributions! Here's how you can help:

### Development Setup

1. **Fork the repository**
2. **Create a feature branch**
   ```bash
   git checkout -b feature/AmazingFeature
   ```
3. **Make your changes**
4. **Test thoroughly**
5. **Commit your changes**
   ```bash
   git commit -m 'Add some AmazingFeature'
   ```
6. **Push to the branch**
   ```bash
   git push origin feature/AmazingFeature
   ```
7. **Open a Pull Request**

### Contribution Guidelines

- 📝 **Code Style**: Follow Java conventions
- 🧪 **Testing**: Test all new features
- 📚 **Documentation**: Update docs for new features
- 🔒 **Security**: Never commit sensitive data

---

## 🐛 Troubleshooting

<details>
<summary>🔥 <b>Common Issues & Solutions</b></summary>

### Firebase Connection Failed
```
Error: Connection to Firebase failed
```
**Solutions:**
- Ensure `firebase-config.json` is properly configured
- Check that Firebase Realtime Database is enabled
- Verify service account permissions

### Class Not Found Errors
```
Error: ClassNotFoundException
```
**Solutions:**
- Ensure all required JAR files are in the `lib/` directory
- Check the classpath in your run configuration
- Verify Java version compatibility

### Profile Images Not Loading
```
Error: Profile image not found
```
**Solutions:**
- Create the `resources/profiles/` directory
- Ensure proper file permissions
- Check image file formats (PNG, JPG supported)

### Email Verification Not Working
```
Error: Email verification failed
```
**Solutions:**
- Check your email spam folder
- Verify email service configuration
- Ensure internet connection is stable

</details>

---

## 📈 Performance & Metrics

- ⚡ **Real-time messaging** with < 100ms latency
- 📱 **Lightweight application** (~50MB footprint)
- 🔄 **Efficient synchronization** with Firebase
- 💾 **Optimized memory usage** with smart caching

---

## 🎯 Roadmap & Future Features

### 🔜 Coming Soon
- [ ] 🎙️ **Voice messaging** support
- [ ] 📁 **File sharing** capabilities
- [ ] 🌙 **Dark mode** theme
- [ ] 🔔 **Push notifications**
- [ ] 🌐 **Multi-language** support

### 🚀 Future Enhancements
- [ ] 📱 **Mobile application** version
- [ ] 🔒 **End-to-end encryption**
- [ ] 📹 **Video calling** integration
- [ ] 🤖 **AI-powered** features
- [ ] 🔗 **Social media** integration

---

## 📊 Project Stats

<div align="center">
  <table>
    <tr>
      <td align="center">
        <img src="https://img.shields.io/github/stars/MohamedLemine1x/AppChat?style=for-the-badge" alt="Stars">
        <br><b>Stars</b>
      </td>
      <td align="center">
        <img src="https://img.shields.io/github/forks/MohamedLemine1x/AppChat?style=for-the-badge" alt="Forks">
        <br><b>Forks</b>
      </td>
      <td align="center">
        <img src="https://img.shields.io/github/issues/MohamedLemine1x/AppChat?style=for-the-badge" alt="Issues">
        <br><b>Issues</b>
      </td>
      <td align="center">
        <img src="https://img.shields.io/github/license/MohamedLemine1x/AppChat?style=for-the-badge" alt="License">
        <br><b>License</b>
      </td>
    </tr>
  </table>
</div>

---

## 📞 Support & Community

<div align="center">
  <table>
    <tr>
      <td align="center">
        <a href="https://github.com/MohamedLemine1x/AppChat/issues">
          <img src="https://img.shields.io/badge/Issues-Report%20Bug-red?style=for-the-badge&logo=github" alt="Issues">
        </a>
      </td>
      <td align="center">
        <a href="https://github.com/MohamedLemine1x/AppChat/discussions">
          <img src="https://img.shields.io/badge/Discussions-Ask%20Question-blue?style=for-the-badge&logo=github" alt="Discussions">
        </a>
      </td>
      <td align="center">
        <a href="mailto:mohamedlemine1x@gmail.com">
          <img src="https://img.shields.io/badge/Email-Contact-orange?style=for-the-badge&logo=gmail" alt="Email">
        </a>
      </td>
    </tr>
  </table>
</div>

---

## 📄 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

```
MIT License

Copyright (c) 2024 Mohamed Lemine

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
```

---

## 🙏 Acknowledgments

- 🔥 **Firebase** - For providing excellent real-time database services
- ☕ **Oracle** - For the Java platform
- 🎨 **UI/UX Inspiration** - Modern chat application designs
- 🚀 **Open Source Community** - For continuous inspiration and support

---

<div align="center">
  <h3>⭐ If you found this project helpful, please give it a star! ⭐</h3>
  <p>Made with ❤️ by <a href="https://github.com/MohamedLemine1x">Mohamed Lemine</a></p>
  
  <i>This application is for educational purposes and demonstrates real-time chat functionality using Firebase and Java Swing.</i>
</div>