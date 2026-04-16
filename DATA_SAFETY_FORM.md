# Google Play Console - Data Safety Form

**App Name:** TaskMaster  
**Package Name:** com.taskmaster.app  
**Developer Name:** [Your Company Name]

---

## Instructions
This form must be filled out in Google Play Console under **App content** → **Data safety section**. Below is a detailed guide for each question with answers specific to TaskMaster.

---

## SECTION 1: DATA COLLECTION & SAFETY

### Question 1.1: Does your app collect or share personal data?
**Answer: YES**

---

## SECTION 2: REQUIRED DISCLOSURES

### Question 2.1: Data Types Collected or Shared

Check all data types your app collects or shares:

#### A. USER-PROVIDED DATA (Information users directly provide)

✅ **Email Address**
- **Collected:** YES
- **Shared:** NO
- **Purpose:** User authentication and account management
- **Retention:** While account is active; deleted within 30 days of account deletion
- **Ephemeral:** NO

✅ **Name**
- **Collected:** YES (Optional)
- **Shared:** NO
- **Purpose:** Account personalization and user identification
- **Retention:** While account is active
- **Ephemeral:** NO

✅ **Photos & Videos**
- **Collected:** YES (Optional - profile picture only)
- **Shared:** NO
- **Purpose:** Profile personalization
- **Retention:** While account is active
- **Ephemeral:** NO

✅ **User-Generated Content**
- **Collected:** YES
- **Shared:** NO
- **Purpose:** Core app functionality (tasks and notes storage)
- **Retention:** While account is active; backed up for 90 days after deletion
- **Ephemeral:** NO

❌ **Phone Number**
- **Collected:** NO (Optional during account recovery setup)
- **Shared:** NO
- **Retention:** N/A
- **Ephemeral:** N/A

#### B. AUTOMATICALLY COLLECTED DATA (Collected through device sensors or app activity)

✅ **Device Identifiers**
- **Collected:** YES
- **Shared:** YES (to Firebase Analytics)
- **Purpose:** User identification, crash reporting, analytics
- **Retention:** Firebase standard retention policy (typically 14 months for analytics)
- **Ephemeral:** NO

✅ **App Activity**
- **Collected:** YES
- **Shared:** YES (to Firebase Analytics)
- **Purpose:** Understanding user behavior, improving app features
- **Retention:** Firebase standard retention
- **Ephemeral:** NO

✅ **Crash Logs**
- **Collected:** YES
- **Shared:** YES (to Firebase Crashlytics)
- **Purpose:** Debugging, identifying and fixing app crashes
- **Retention:** Firebase default (typically 90 days)
- **Ephemeral:** NO

✅ **App Performance Data**
- **Collected:** YES
- **Shared:** YES (to Firebase Performance Monitoring)
- **Purpose:** Monitoring app stability and performance
- **Retention:** Firebase standard retention
- **Ephemeral:** NO

✅ **Installation Data**
- **Collected:** YES
- **Shared:** YES (to Firebase Analytics)
- **Purpose:** Analytics and tracking app installations
- **Retention:** Firebase standard retention
- **Ephemeral:** NO

---

### Question 2.2: Data Sharing Details

For each data type collected, specify:

**Email Address**
- Shared with: Google Firebase
- Purpose: Authentication and account management
- Reason for sharing: Essential service provider for app functionality
- Link to their privacy policy: https://policies.google.com/privacy

**User Generated Content (Tasks & Notes)**
- Shared with: Google Firebase Firestore
- Purpose: Cloud data storage and synchronization
- Reason for sharing: Essential service provider for core functionality
- Link to their privacy policy: https://policies.google.com/privacy

**Device Identifiers**
- Shared with: Google Firebase
- Purpose: Crash reporting, analytics, user identification
- Reason for sharing: Essential service provider for app stability and improvement
- Link to their privacy policy: https://policies.google.com/privacy

**App Activity & Crash Logs**
- Shared with: Google Firebase Services
- Purpose: Analytics, performance monitoring, crash reporting
- Reason for sharing: Essential service provider for identifying bugs and improving app
- Link to their privacy policy: https://policies.google.com/privacy

---

### Question 2.3: User Data Practices

#### Encryption in Transit
**Question:** Does your app encrypt data in transit?
**Answer: YES**
- All data transmitted between the app and Firebase servers uses TLS/HTTPS encryption
- Secure connection is enforced for all network communications

#### Encryption at Rest
**Question:** Does your app encrypt data at rest?
**Answer: YES**
- Firebase Firestore encrypts all data at rest with Google-managed keys
- User passwords are hashed and encrypted in the database

#### Secure Data Deletion
**Question:** Does your app delete user data upon request?
**Answer: YES**
- Users can request account deletion through the app settings or by contacting support
- All personal data and user-generated content is deleted within 30 days of account deletion
- Backup copies may persist for up to 90 days in Firebase recovery systems

#### Data Access Restrictions
**Question:** Does your app restrict data access by user?
**Answer: YES**
- User data is strictly isolated per user account
- Users can only access their own tasks, notes, and account information
- Cloud Firestore security rules enforce authentication and user-specific access

---

## SECTION 3: SECURITY PRACTICES

### Question 3.1: Security & Privacy Practices

✅ **App Security Certification**
- We have not obtained a security certification (e.g., ISO 27001)
- [Select if applicable to your company]

✅ **App Privacy Policy**
- Link to Privacy Policy: [Your website URL]/privacy-policy
- The policy covers all data collection, use, and sharing practices

✅ **App Permissions**
- We only request permissions necessary for core functionality
- Users are informed of permission usage in the app

✅ **Third-Party SDKs**
- All third-party SDKs and libraries used are from trusted providers (Google Firebase)
- We regularly monitor and update dependencies

---

## SECTION 4: APP PERMISSIONS JUSTIFICATION

List all dangerous permissions your app requests and justify each:

### 1. SCHEDULE_EXACT_ALARM
- **Purpose:** Schedule and deliver task reminders and alarms at specific times
- **Justification:** Core feature - users rely on precise alarm delivery
- **Data Accessed:** None (only used to schedule alarms)
- **Alternatives Considered:** Inexact alarms provide unreliable user experience

### 2. USE_EXACT_ALARM
- **Purpose:** Accurate alarm delivery on newer Android versions
- **Justification:** Core feature - maintains alarm functionality
- **Data Accessed:** None
- **Alternatives Considered:** Less precise alarm delivery

### 3. RECEIVE_BOOT_COMPLETED
- **Purpose:** Restore alarms and reminders after device restart
- **Justification:** User expectation - alarms should persist across restarts
- **Data Accessed:** Locally stored alarm schedules
- **Alternatives Considered:** Alarms would be lost on device restart

### 4. VIBRATE
- **Purpose:** Provide haptic feedback for alarm notifications
- **Justification:** Enhanced user experience and accessibility
- **Data Accessed:** None
- **Alternatives Considered:** Audio-only notifications less effective

### 5. POST_NOTIFICATIONS
- **Purpose:** Send task reminders, due date alerts, and alarm notifications
- **Justification:** Core feature - users need timely notifications
- **Data Accessed:** Task titles, due dates, reminder times
- **Alternatives Considered:** App would be non-functional without notifications

### 6. WAKE_LOCK
- **Purpose:** Keep device awake during critical alarm operations
- **Justification:** Ensures alarms trigger even if device is in low-power mode
- **Data Accessed:** None
- **Alternatives Considered:** Alarms may fail silently without wake lock

### 7. USE_FULL_SCREEN_INTENT
- **Purpose:** Display full-screen alarm notifications
- **Justification:** Ensures users don't miss important alarms
- **Data Accessed:** Alarm details
- **Alternatives Considered:** Standard notifications may be missed

### 8. INTERNET
- **Purpose:** Connect to Firebase servers for authentication and data sync
- **Justification:** Cloud synchronization and account management
- **Data Accessed:** All user data that needs syncing
- **Alternatives Considered:** App would be offline-only without this

---

## SECTION 5: NON-DISCLOSURE & RESTRICTIONS

### Question 5.1: Data Selling & Restriction

**Question:** Does your app sell user data?
**Answer: NO**
- We do not sell, rent, or lease any user personal data
- We do not share user data with third parties for marketing purposes

**Question:** Does your app restrict data based on user age?
**Answer: YES**
- Children under 13 years of age are restricted from certain features
- We do not knowingly collect data from children under 13
- Parents/guardians can request deletion of a child's account

---

## SECTION 6: USER DATA RIGHTS

### Question 6.1: User Rights & Options

**Data Access**
- Users can download their data through account settings
- Option to export tasks and notes

**Data Deletion**
- Users can delete individual tasks/notes
- Users can request complete account deletion
- Data deleted within 30 days

**Data Correction**
- Users can edit their profile information
- Users can update task and note content

**Opt-Out Rights**
- Users can disable notifications
- Users can opt-out of analytics
- Users can manage push notification preferences

---

## SECTION 7: CONTACT INFORMATION

**Primary Contact Name:** [Your Name]  
**Email:** [your-email@company.com]  
**Phone:** [Your Phone Number]  
**Company:** [Your Company Name]  
**Address:** [Your Company Address]  

**Data Protection Officer (if applicable):**  
Name: [DPO Name]  
Email: [DPO Email]  

---

## SECTION 8: IMPORTANT NOTES

✅ **Accuracy Certification**
- I certify that the information provided in this Data Safety form is accurate and complete
- I understand that providing false or misleading information violates Google Play policies
- I am authorized to provide this information on behalf of the app developer

✅ **Policy Compliance**
- This app complies with Google Play's Families Policy
- This app complies with Google Play's Data Safety requirements
- All third-party SDKs comply with Google Play policies

✅ **Regular Updates**
- This form will be updated whenever the app's data practices change
- Privacy practices are reviewed quarterly
- Changes will be updated in the Play Console within 30 days of implementation

---

## SUBMISSION CHECKLIST

- [ ] Privacy Policy URL is complete and accessible
- [ ] All data types collected are listed accurately
- [ ] All sharing practices are disclosed
- [ ] All dangerous permissions are justified
- [ ] Encryption practices are documented
- [ ] User rights are clearly explained
- [ ] Contact information is up-to-date
- [ ] Form has been reviewed for accuracy
- [ ] Form submitted to Google Play Console

---

**Last Updated:** April 15, 2026  
**Next Review Date:** July 15, 2026

---

*End of Data Safety Form*
