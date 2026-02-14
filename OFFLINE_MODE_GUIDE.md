# 🔧 SKILORA - DATABASE CONNECTION & OFFLINE MODE GUIDE

**Date:** February 11, 2026  
**Status:** Application now supports both ONLINE (with MySQL) and OFFLINE modes

---

## 📋 Quick Start

### ✅ Option 1: Run WITH Database (Recommended)

#### Step 1: Start MySQL Server
**Windows with XAMPP:**
```
1. Open XAMPP Control Panel
2. Click "Start" next to MySQL
3. Wait for it to turn green (running)
```

**Windows with MySQL Service:**
```PowerShell
# In PowerShell (as Administrator)
net start MySQL80

# Or use START_MYSQL.bat script:
# Double-click: c:\Users\21625\Downloads\JAVAFX11\JAVAFX\START_MYSQL.bat
```

**macOS:**
```bash
brew services start mysql
```

**Linux:**
```bash
sudo systemctl start mysql
# or
sudo service mysql start
```

#### Step 2: Create Database
```sql
mysql -u root

CREATE DATABASE skilora CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
EXIT;
```

#### Step 3: Run Application
```bash
cd c:\Users\21625\Downloads\JAVAFX11\JAVAFX
mvn javafx:run
```

**Expected Output:**
```
✅ Database connected successfully
```

---

### ⏳ Option 2: Run WITHOUT Database (Offline Mode)

**How it Works:**
- Application starts even if MySQL is not running
- Shows: `⚠️  OFFLINE MODE (Database unavailable)`
- UI loads but data features are limited
- Perfect for UI testing/development

**Just Run:**
```bash
cd c:\Users\21625\Downloads\JAVAFX11\JAVAFX
mvn javafx:run
```

**Console Output (Offline):**
```
⚠️  WARNING: Database connection failed (OFFLINE MODE ENABLED)
   URL: jdbc:mysql://localhost:3306/skilora?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
   Error: Connection refused: getsockopt

   ℹ️  To fix:
   1. Start MySQL server (XAMPP or MySQL service)
   2. Create database: CREATE DATABASE skilora;
   3. Restart the application

   The app will run in OFFLINE mode (limited functionality)

⏭️  Skipping database initialization (offline mode)
```

---

## 🔄 Switching Between Modes

### From Offline to Online

**Step 1:** Start MySQL (see Option 1 above)

**Step 2:** Restart the application
```bash
# Kill current app (Ctrl+C)
# Then run:
mvn javafx:run
```

**Step 3:** Should now show:
```
✅ Database connected successfully
```

### From Online to Offline

Simply stop the MySQL server:

**Windows:**
```PowerShell
net stop MySQL80
```

**macOS:**
```bash
brew services stop mysql
```

---

## 🎯 What Works in Each Mode

| Feature | Online ✅ | Offline ⏳ |
|---------|----------|-----------|
| Login Screen | ✅ Full | ⚠️ UI Only |
| User Registration | ✅ Yes | ❌ No |
| Dashboard | ✅ Full | ⚠️ UI Only |
| Finance Module | ✅ Full | ⚠️ UI Only |
| Data Saving | ✅ Yes | ❌ No |
| Payslip Generation | ✅ Yes | ❌ No |
| PDF Export | ✅ Yes | ⚠️ Demo |
| Real-time Sync | ✅ Yes | ❌ No |

---

## 🛠️ Troubleshooting

### Issue: "Offline Mode" when MySQL is running

**Cause:** MySQL server not responding on port 3306

**Solutions:**

1. **Verify MySQL is running:**
```bash
# Windows - Check if service is active
net start | findstr MySQL

# macOS - Check if running
brew services list | grep mysql

# Linux
systemctl status mysql
```

2. **Check port 3306 is listening:**
```bash
# Windows
netstat -ano | findstr :3306

# macOS/Linux
lsof -i :3306
```

3. **Restart MySQL:**
```bash
# Windows
net stop MySQL80
net start MySQL80

# macOS
brew services restart mysql

# Linux
sudo systemctl restart mysql
```

4. **Database credentials wrong:**
   - Default: `root` with empty password
   - Check in `DatabaseConfig.java`:
```java
private static final String DEFAULT_USER = "root";
private static final String DEFAULT_PASSWORD = "";
```

### Issue: "Connection refused"

**Cause:** MySQL is not listening on port 3306

**Solutions:**

1. Check MySQL is actually running:
```bash
ps aux | grep mysqld   # macOS/Linux
tasklist | grep mysql  # Windows
```

2. Check my.ini configuration:
```
port = 3306
bind-address = 127.0.0.1
```

3. Restart with explicit port:
```bash
mysqld --port=3306
```

### Issue: Database exists but tables not created

**Cause:** Database initialized but tables missing

**Solution:** Delete database and restart app:
```sql
DROP DATABASE skilora;
-- Restart app - will auto-create
```

---

## 📊 Database Architecture

### Connection Pool Settings

```java
// Maximum pool size: 10 connections
// Minimum idle: 2 connections
// Connection timeout: 3 seconds (fast fail)
// Idle timeout: 30 seconds
```

### Auto-Reconnection

If connection is lost:
1. App detects failure (3-second timeout)
2. Switches to graceful offline mode
3. Shows warning message
4. User can restart app after MySQL starts

---

## 🚀 Advanced Setup

### Environment Variables

Override default database settings:

**Windows:**
```PowerShell
$env:SKILORA_DB_URL = "jdbc:mysql://192.168.1.100:3306/skilora"
$env:SKILORA_DB_USER = "skilora_user"
$env:SKILORA_DB_PASSWORD = "secure_password"
mvn javafx:run
```

**macOS/Linux:**
```bash
export SKILORA_DB_URL="jdbc:mysql://192.168.1.100:3306/skilora"
export SKILORA_DB_USER="skilora_user"
export SKILORA_DB_PASSWORD="secure_password"
mvn javafx:run
```

### Docker Setup

**Using Docker Compose:**

Create `docker-compose.yml`:
```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    container_name: skilora-mysql
    environment:
      MYSQL_ROOT_PASSWORD: password
      MYSQL_DATABASE: skilora
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
    command: mysqld --default-authentication-plugin=mysql_native_password

volumes:
  mysql_data:
```

**Run:**
```bash
docker-compose up -d

# Verify
docker-compose logs mysql
```

### Remote Database

Connect to remote MySQL server:

```PowerShell
$env:SKILORA_DB_URL = "jdbc:mysql://remote-server.com:3306/skilora?useSSL=true"
$env:SKILORA_DB_USER = "remote_user"
$env:SKILORA_DB_PASSWORD = "remote_password"
mvn javafx:run
```

---

## 📝 Connection Debugging

### Enable Debug Logging

```bash
mvn javafx:run -Ddebug=true
```

### Check Connection Details

The app logs connection info on startup:

```
✅ Database connected successfully
   URL: jdbc:mysql://localhost:3306/skilora
   User: root
```

### Test Direct Connection

```bash
# Test connection from command line
mysql -h localhost -u root -p skilora

# If successful, data should be available:
SHOW TABLES;
SELECT COUNT(*) FROM users;
```

---

## 🔐 Security Notes

### Default Credentials

⚠️ **Development Only:**
```
Host: localhost
Port: 3306
User: root
Password: (empty)
```

### For Production

Change credentials:

```sql
-- Create dedicated user
CREATE USER 'skilora_app'@'localhost' IDENTIFIED BY 'strong_password';
GRANT ALL PRIVILEGES ON skilora.* TO 'skilora_app'@'localhost';
FLUSH PRIVILEGES;
```

Use environment variables:
```bash
export SKILORA_DB_USER="skilora_app"
export SKILORA_DB_PASSWORD="strong_password"
```

---

## ✅ Verification Checklist

Before considering setup complete:

- [ ] MySQL server installed and running
- [ ] `skillora` database created
- [ ] Can connect to MySQL from command line
- [ ] Application starts without database errors
- [ ] Login page loads
- [ ] Finance module accessible
- [ ] Can perform database operations (if online)

---

## 🆘 Get Help

### Check Logs

**Application console:**
```
Look for lines starting with:
✅ = Success
⚠️  = Warning (offline mode)
❌ = Error (connection failed)
```

**MySQL logs:**

**Windows XAMPP:**
```
C:\xampp\mysql\data\[hostname].err
```

**Linux:**
```
/var/log/mysql/error.log
```

**macOS:**
```
/usr/local/var/mysql/[hostname].err
```

### Common Error Messages

| Message | Meaning | Fix |
|---------|---------|-----|
| `Connection refused: getsockopt` | MySQL not listening | Start MySQL |
| `Authentication failed` | Wrong password | Check credentials |
| `Database 'skilora' doesn't exist` | DB not created | Create database |
| `OFFLINE MODE` | No DB connection | Start MySQL |

---

## 📞 Support Resources

1. **MySQL Documentation:** https://dev.mysql.com/doc/
2. **XAMPP Support:** https://www.apachefriends.org/
3. **Docker MySQL:** https://hub.docker.com/_/mysql
4. **Connection Troubleshooting:** https://dev.mysql.com/doc/refman/8.0/en/troubleshooting.html

---

## 🎉 Summary

Skilora now operates in two modes:

1. **Online Mode (with MySQL):** Full functionality, persistent data
2. **Offline Mode (without MySQL):** UI accessible, limited functionality

**Start MySQL:**
1. XAMPP: Click "Start"
2. Windows Service: `net start MySQL80`
3. Script: Run `START_MYSQL.bat`

**Run App:**
```bash
mvn javafx:run
```

**Status:** The application will automatically detect database availability and show appropriate messages.

---

**Last Updated:** February 11, 2026  
**Status:** ✅ Offline mode fully functional
